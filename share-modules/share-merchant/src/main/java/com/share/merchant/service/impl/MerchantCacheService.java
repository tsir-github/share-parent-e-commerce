package com.share.merchant.service.impl;

import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.MqConstants;
import com.share.common.redis.service.RedisService;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.service.IMerchantCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商家信息缓存服务 — Cache-Aside + MQ 延迟双删
 *
 * <p>缓存商家基本信息，TTL 5min ± 20% 随机偏移防雪崩。
 * 商家信息变化频率极低（几天甚至几周不变），短 TTL 进一步降低脏数据窗口。</p>
 *
 * <p>延迟双删走 MQ 延迟消息（延迟级别3=10s），替代 daemon 线程方案：
 * MQ 保证投递、JVM 重启不丢、天然削峰。</p>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantCacheService implements IMerchantCacheService {

    private final RedisService redisService;
    private final RocketMQTemplate rocketMQTemplate;

    private static final long TTL_MINUTES = 5;
    private static final double TTL_JITTER = 0.2;

    public MerchantInfo getMerchant(Long merchantId) {
        String cacheKey = CacheConstants.MERCHANT_INFO_KEY + merchantId;
        try {
            return redisService.getCacheObject(cacheKey);
        } catch (Exception e) {
            log.warn("商家信息缓存读取异常，回源查库: merchantId={}", merchantId, e);
            return null;
        }
    }

    public void setMerchant(Long merchantId, MerchantInfo merchant) {
        String cacheKey = CacheConstants.MERCHANT_INFO_KEY + merchantId;
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(cacheKey, merchant, ttl, TimeUnit.MINUTES);
            log.debug("商家信息缓存写入: merchantId={}", merchantId);
        } catch (Exception e) {
            log.warn("商家信息缓存写入失败: merchantId={}", merchantId, e);
        }
    }

    public void evictMerchant(Long merchantId) {
        String cacheKey = CacheConstants.MERCHANT_INFO_KEY + merchantId;
        // 先删缓存（失败抛异常 → 调用方 @Transactional 回滚）
        redisService.deleteObject(cacheKey);
        // MQ 异步延迟消息兜底（Broker 侧 10s 投递消费者二次删除，不阻塞调用线程）
        rocketMQTemplate.asyncSend(
                MqConstants.CACHE_INVALIDATE_TOPIC,
                MessageBuilder.withPayload(cacheKey)
                        .setHeader("DELAY", 3)
                        .build(),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.debug("MQ缓存失效投递成功: key={}", cacheKey);
                    }
                    @Override
                    public void onException(Throwable e) {
                        log.warn("MQ缓存失效投递失败，尝试syncSend重试: key={}", cacheKey, e);
                        try {
                            rocketMQTemplate.syncSend(MqConstants.CACHE_INVALIDATE_TOPIC,
                                    MessageBuilder.withPayload(cacheKey)
                                            .setHeader("DELAY", 3)
                                            .build(),
                                    3000, 3);
                            log.debug("MQ缓存失效重试投递成功: key={}", cacheKey);
                        } catch (Exception ex) {
                            log.error("MQ缓存失效重试也失败，放弃: key={}", cacheKey, ex);
                        }
                    }
                });
    }

    private long ttlWithJitter(long baseMinutes) {
        return (long) (baseMinutes * (0.8 + TTL_JITTER * 2 * Math.random()));
    }
}
