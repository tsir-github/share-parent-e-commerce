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
import com.share.goods.service.ISkuCacheService;
import com.share.common.core.constant.CacheConstants;
import com.share.goods.service.IRedisStockService;
import org.springframework.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final ISkuCacheService skuCacheService;

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
        boolean result = super.save(entity);
        if (result && entity.getId() != null) {
            skuCacheService.evictSku(entity.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(ProductSku entity) {
        boolean result = super.updateById(entity);
        if (result && entity.getId() != null) {
            skuCacheService.evictSku(entity.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(ProductSku entity) {
        boolean result = super.removeById(entity);
        if (result && entity.getId() != null) {
            // ponytail: 删除 SKU 时驱逐 info 缓存 + 清除 Redis 库存，防止 InnerSkuController 缓存穿透返回已删除的 SKU
            skuCacheService.evictSku(entity.getId());
            redisStockService.deleteStock(entity.getId());
        }
        return result;
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
        RLock lock = redissonClient.getLock(CacheConstants.STOCK_SKU_LOCK_KEY + skuId);
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
                    // DB 扣减成功 → 同步 Redis 快照 + 驱逐 SKU 信息缓存
                    ProductSku updated = productSkuMapper.selectById(skuId);
                    redisStockService.syncStock(skuId, updated.getStock());
                    // ponytail: SKU 缓存失效失败不影响库存变更（库存数据已在 DB/Redis 正确更新）
                    try {
                        skuCacheService.evictSku(skuId);
                    } catch (Exception e) {
                        log.warn("库存扣减后SKU缓存失效失败: skuId={}", skuId, e);
                    }
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
    public boolean deductStockBatch(List<RemoteGoodsService.StockDeductDTO> items, String orderNo) {
        // 请求级幂等：goods 侧 Redis SETNX 防同一订单重复扣
        List<String> dedupKeys = new ArrayList<>();
        try {
            for (RemoteGoodsService.StockDeductDTO item : items) {
                String dedupKey = CacheConstants.STOCK_DEDUCT_DEDUP_KEY + orderNo + ":" + item.getSkuId();
                if (!redissonClient.getBucket(dedupKey).trySet("1", 7, TimeUnit.DAYS)) {
                    log.info("重复扣减跳过（goods侧幂等）: orderNo={}, skuId={}", orderNo, item.getSkuId());
                    continue;
                }
                dedupKeys.add(dedupKey);
                if (!deductStock(item.getSkuId(), item.getQuantity())) {
                    throw new ServiceException("扣减库存失败: skuId=" + item.getSkuId());
                }
            }
            return true;
        } catch (Exception e) {
            // DB 事务回滚 → 清理已设的 Redis 幂等标记，允许后续重试
            for (String key : dedupKeys) {
                redissonClient.getBucket(key).delete();
            }
            throw e;
        }
    }

    // ==================== 归还库存 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStockBatch(List<RemoteGoodsService.StockDeductDTO> items, String orderNo) {
        // 请求级幂等：goods 侧 Redis SETNX 防同一订单重复归还
        List<String> dedupKeys = new ArrayList<>();
        try {
            for (RemoteGoodsService.StockDeductDTO item : items) {
                String dedupKey = CacheConstants.STOCK_RELEASE_DEDUP_KEY + orderNo + ":" + item.getSkuId();
                if (!redissonClient.getBucket(dedupKey).trySet("1", 7, TimeUnit.DAYS)) {
                    log.info("重复归还跳过（goods侧幂等）: orderNo={}, skuId={}", orderNo, item.getSkuId());
                    continue;
                }
                dedupKeys.add(dedupKey);
                releaseStock(item.getSkuId(), item.getQuantity());
            }
        } catch (Exception e) {
            // DB 事务回滚 → 清理已设的 Redis 幂等标记，允许后续重试
            for (String key : dedupKeys) {
                redissonClient.getBucket(key).delete();
            }
            throw e;
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
                    // ponytail: SKU 缓存失效失败不影响库存变更（库存数据已在 DB/Redis 正确更新）
                    try {
                        skuCacheService.evictSku(skuId);
                    } catch (Exception e) {
                        log.warn("库存归还后SKU缓存失效失败: skuId={}", skuId, e);
                    }
                    return true;
            }
        }
        log.error("归还库存失败（乐观锁冲突）: skuId={}, qty={}", skuId, quantity);
        // ponytail: 归还失败不抛异常，记录日志 + 人工补偿
        return false;
    }
}
