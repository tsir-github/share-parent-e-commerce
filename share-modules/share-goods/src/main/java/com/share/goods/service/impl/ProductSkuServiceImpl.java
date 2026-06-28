package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.goods.api.RemoteGoodsService;
import com.share.goods.domain.ProductSku;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.share.common.core.constant.ProductSkuStatus;
import com.share.common.core.exception.ServiceException;
import com.share.goods.mapper.ProductSkuMapper;
import com.share.goods.service.IProductSkuService;
import com.share.goods.service.IRedisStockService;
import org.springframework.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品 SKU Service 业务层处理
 *
 * <p>库存扣减采用三级防护：</p>
 * <ol>
 *   <li><b>SKU 级分布式锁</b> — 序列化同一 SKU 的并发扣减请求</li>
 *   <li><b>Redis Lua 预扣</b> — 原子检查+扣减，拦截大部分无效请求</li>
 *   <li><b>DB 乐观锁</b> — {@code WHERE version=#{version} AND stock>=#{quantity}} 兜底</li>
 * </ol>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements IProductSkuService {

    private final ProductSkuMapper productSkuMapper;
    private final IRedisStockService redisStockService;
    private final RedissonClient redissonClient;

    private static final String STOCK_LOCK_PREFIX = "stock:sku:lock:";
    private static final Long DEFAULT_MERCHANT_ID = 1L;

    public List<ProductSku> selectSkuList(ProductSku sku) {
        return productSkuMapper.selectSkuList(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ProductSku entity) {
        if (entity.getMerchantId() == null) {
            entity.setMerchantId(DEFAULT_MERCHANT_ID);
        }
        return super.save(entity);
    }

    public List<ProductSku> selectSkuByProductId(Long productId) {
        return productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getDelFlag, "0")
                        .eq(ProductSku::getStatus, ProductSkuStatus.ENABLED));
    }

    @Override
    public ProductSku getSkuById(Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new ServiceException("SKU不存在: " + skuId);
        }
        return sku;
    }

    // ==================== 三级防护：扣减库存 ====================

    @Override
    @SentinelResource(value = "deductStock", fallback = "deductStockFallback")
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long skuId, Integer quantity) {
        // L1: SKU 级分布式锁 — 序列化同一 SKU 的并发扣减
        RLock lock = redissonClient.getLock(STOCK_LOCK_PREFIX + skuId);
        try {
            if (!lock.tryLock(2, 10, TimeUnit.SECONDS)) {
                throw new ServiceException("库存操作繁忙，请重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("库存操作被中断");
        }

        boolean redisDeducted = false;
        try {
            // L2: Redis Lua 预扣 — 原子检查+扣减
            if (!redisStockService.preDeduct(skuId, quantity)) {
                throw new ServiceException("库存不足");
            }
            redisDeducted = true;

            // L3: DB 乐观锁扣减 — 最多重试 3 次
            for (int i = 0; i < 3; i++) {
                ProductSku sku = productSkuMapper.selectById(skuId);
                if (sku == null) {
                    throw new ServiceException("SKU不存在");
                }
                int rows = productSkuMapper.deductStock(skuId, quantity, sku.getVersion());
                if (rows > 0) {
                    // DB 扣减成功 → 同步 Redis 快照
                    ProductSku updated = productSkuMapper.selectById(skuId);
                    redisStockService.syncStock(skuId, updated.getStock());
                    return true;
                }
            }
            // DB 乐观锁 3 次全冲突 → 回滚 Redis
            throw new ServiceException("扣库存失败，并发冲突，请重试");
        } catch (Exception e) {
            // Redis 已扣但 DB 失败 → 回滚 Redis
            if (redisDeducted) {
                redisStockService.preRelease(skuId, quantity);
            }
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * deductStock 的 Sentinel 降级方法（仅限流/熔断时触发）
     */
    public boolean deductStockFallback(Long skuId, Integer quantity, Throwable t) {
        log.error("扣库存被限流降级: skuId={}, error={}", skuId, t.getMessage());
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockBatch(List<RemoteGoodsService.StockDeductDTO> items) {
        for (RemoteGoodsService.StockDeductDTO item : items) {
            if (!deductStock(item.getSkuId(), item.getQuantity())) {
                throw new ServiceException("扣减库存失败: skuId=" + item.getSkuId());
            }
        }
        return true;
    }

    // ==================== 归还库存 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStockBatch(List<RemoteGoodsService.StockDeductDTO> items) {
        for (RemoteGoodsService.StockDeductDTO item : items) {
            releaseStock(item.getSkuId(), item.getQuantity());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseStock(Long skuId, Integer quantity) {
        // Redis 归还（幂等，不会扣成负数）
        redisStockService.preRelease(skuId, quantity);

        // DB 乐观锁归还 — 最多重试 3 次
        for (int i = 0; i < 3; i++) {
            ProductSku sku = productSkuMapper.selectById(skuId);
            if (sku == null) {
                throw new ServiceException("SKU不存在");
            }
            int rows = productSkuMapper.releaseStock(skuId, quantity, sku.getVersion());
            if (rows > 0) {
                ProductSku updated = productSkuMapper.selectById(skuId);
                redisStockService.syncStock(skuId, updated.getStock());
                return true;
            }
        }
        log.error("归还库存失败（乐观锁冲突）: skuId={}, qty={}", skuId, quantity);
        // ponytail: 归还失败不抛异常，记录日志 + 人工补偿
        return false;
    }
}
