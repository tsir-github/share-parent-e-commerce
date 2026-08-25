package com.share.merchant.config;

import com.share.common.core.constant.MqConstants;
import com.share.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 缓存失效 MQ 消费者 — 延迟双删的可靠兜底
 *
 * <p>收到消息后执行 Redis key 删除。由于所有模块共享同一 Redis 集群，
 * merchant 侧的消费者也可以删除 goods 侧的缓存 key（key 都在同一个 Sentinel 集群中）。</p>
 *
 * @author share
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.CACHE_INVALIDATE_TOPIC,
        consumerGroup = "cache-invalidate-consumer-group"
)
public class CacheInvalidateConsumer implements RocketMQListener<String> {

    private final RedisService redisService;

    @Override
    public void onMessage(String cacheKey) {
        for (int i = 0; i < 3; i++) {
            try {
                redisService.deleteObject(cacheKey);
                log.debug("MQ延迟缓存失效: key={}", cacheKey);
                return;
            } catch (Exception e) {
                if (i < 2) {
                    // ponytail: 短暂等待后重试，应对 Redis 瞬断
                    try {
                        Thread.sleep(100L * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("MQ缓存失效失败（已重试3次）: key={}", cacheKey, e);
                }
            }
        }
    }
}
