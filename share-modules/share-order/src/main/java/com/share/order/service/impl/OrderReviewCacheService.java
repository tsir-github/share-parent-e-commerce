package com.share.order.service.impl;

import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.MqConstants;
import com.share.common.redis.service.RedisService;
import com.share.order.domain.vo.ReviewStatsDTO;
import com.share.order.service.IOrderReviewCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 评价统计缓存服务 — Cache-Aside + MQ 延迟双删
 *
 * <p>缓存商品评价统计数据（平均分、评价数），TTL 10min ± 20% 随机偏移防雪崩。</p>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReviewCacheService implements IOrderReviewCacheService {

    private final RedisService redisService;
    private final RocketMQTemplate rocketMQTemplate;

    private static final long TTL_MINUTES = 10;
    private static final double TTL_JITTER = 0.2;

    public ReviewStatsDTO getStats(Long productId) {
        String cacheKey = CacheConstants.REVIEW_STATS_KEY + productId;
        try {
            ReviewStatsDTO cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("评价统计缓存读取异常，回源查库: productId={}", productId, e);
        }
        return null;
    }

    public void setStats(Long productId, ReviewStatsDTO stats) {
        String cacheKey = CacheConstants.REVIEW_STATS_KEY + productId;
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(cacheKey, stats, ttl, TimeUnit.MINUTES);
            log.debug("评价统计缓存写入: productId={}", productId);
        } catch (Exception e) {
            log.warn("评价统计缓存写入失败: productId={}", productId, e);
        }
    }

    public void evictStats(Long productId) {
        String cacheKey = CacheConstants.REVIEW_STATS_KEY + productId;
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

    /**
     * TTL 随机偏移，避免缓存雪崩
     */
    private long ttlWithJitter(long baseMinutes) {
        return (long) (baseMinutes * (0.8 + TTL_JITTER * 2 * Math.random()));
    }
}
