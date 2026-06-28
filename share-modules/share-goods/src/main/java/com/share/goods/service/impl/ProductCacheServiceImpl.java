package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.ProductStatus;
import com.share.common.redis.service.RedisService;
import com.share.goods.domain.Product;
import com.share.goods.domain.ProductSku;
import com.share.goods.service.IProductCacheService;
import com.share.goods.service.IProductService;
import com.share.goods.service.IProductSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 商品缓存服务 — Cache-Aside + 延迟双删
 *
 * <p>读：缓存命中返回，未命中查 DB → 写缓存（TTL 30min）
 * 写：立即删缓存 → 更新 DB → 延迟 500ms 再删一次</p>
 *
 * <p>延迟双删防止并发读写导致的脏缓存：</p>
 * <pre>
 * 线程A（读）:  miss → 读DB旧数据 → 写缓存旧数据
 * 线程B（写）:  立即删 → 更新DB → 延迟删（清理A写入的脏数据）
 * </pre>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheServiceImpl implements IProductCacheService {

    private final IProductService productService;
    private final IProductSkuService productSkuService;
    private final RedisService redisService;

    private static final long TTL_MINUTES = 30;
    /** 延迟双删间隔（毫秒） */
    private static final long DOUBLE_DELETE_DELAY_MS = 500;
    /** 单线程调度器，延迟删低频操作 */
    private static final ScheduledExecutorService DELAYED_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cache-double-delete");
                t.setDaemon(true);
                return t;
            });

    @Override
    public Product getProductWithCache(Long id) {
        String cacheKey = CacheConstants.PRODUCT_DETAIL_KEY + id;
        Product cached = redisService.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }
        Product product = productService.getById(id);
        if (product == null || !ProductStatus.LISTED.equals(product.getStatus())) {
            return null;
        }
        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, id);
        product.getParams().put("skus", productSkuService.list(skuWrapper));

        redisService.setCacheObject(cacheKey, product, TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("商品缓存写入: id={}, key={}", id, cacheKey);
        return product;
    }

    @Override
    public void evictProductCache(Long id) {
        String cacheKey = CacheConstants.PRODUCT_DETAIL_KEY + id;
        // 第一次删：立即失效
        redisService.deleteObject(cacheKey);
        // 第二次删：延迟 500ms 再删，清理并发读写入的脏数据
        DELAYED_EXECUTOR.schedule(() -> {
            redisService.deleteObject(cacheKey);
            log.debug("商品缓存延迟双删: id={}", id);
        }, DOUBLE_DELETE_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}
