package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.MqConstants;
import com.share.common.core.constant.ProductStatus;
import com.share.common.redis.service.RedisService;
import com.share.goods.domain.Product;
import com.share.goods.domain.ProductSku;
import com.share.goods.service.IProductCacheService;
import com.share.goods.service.IProductService;
import com.share.goods.service.IProductSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品缓存服务 — Cache-Aside + MQ 延迟双删
 *
 * <p>读：缓存命中返回，未命中查 DB → 写缓存（TTL 5min）
 * 写：立即删缓存 → 更新 DB → MQ 延迟消息再删一次（delayLevel 3=10s）</p>
 *
 * <p>延迟双删防止并发读写导致的脏缓存。MQ 方案替代 daemon 线程：
 * 保证投递、JVM 重启不丢、天然削峰。</p>
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
    private final RocketMQTemplate rocketMQTemplate;

    private static final long TTL_MINUTES = 5;
    private static final double TTL_JITTER = 0.2;

    @Override
    public Product getProductWithCache(Long id) {
        String cacheKey = CacheConstants.PRODUCT_DETAIL_KEY + id;
        try {
            Product cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("商品缓存读取异常，回源查库: id={}", id, e);
        }
        Product product = productService.getById(id);
        if (product == null || !ProductStatus.LISTED.equals(product.getStatus())) {
            return null;
        }
        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, id);
        product.getParams().put("skus", productSkuService.list(skuWrapper));

        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(cacheKey, product, ttl, TimeUnit.MINUTES);
            log.debug("商品缓存写入: id={}, key={}", id, cacheKey);
        } catch (Exception e) {
            log.warn("商品缓存写入失败: id={}", id, e);
        }
        return product;
    }

    @Override
    public void evictProductCache(Long id) {
        String cacheKey = CacheConstants.PRODUCT_DETAIL_KEY + id;
        // MQ 先发 → 保证兜底一定存在
        try {
            rocketMQTemplate.syncSend(MqConstants.CACHE_INVALIDATE_TOPIC,
                    MessageBuilder.withPayload(cacheKey).build(),
                    3000, 3);
        } catch (Exception e) {
            log.warn("MQ缓存失效投递失败，跳过本次驱逐: key={}", cacheKey, e);
            return;
        }
        // MQ 已投递 → 安全删缓存
        redisService.deleteObject(cacheKey);
    }

    private long ttlWithJitter(long baseMinutes) {
        return (long) (baseMinutes * (0.8 + TTL_JITTER * 2 * Math.random()));
    }
}
