package com.share.order.config;

import com.share.common.core.constant.MqConstants;
import com.share.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 缓存失效 MQ 消费者（order 侧兜底）
 *
 * <p>与 goods 侧消费者共用同一 consumerGroup，RocketMQ 集群模式负载均衡。
 * goods 模块挂掉时此消费者自动接管所有缓存失效消息。</p>
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
        try {
            redisService.deleteObject(cacheKey);
            log.debug("MQ延迟缓存失效(order): key={}", cacheKey);
        } catch (Exception e) {
            log.error("MQ缓存失效失败(order): key={}", cacheKey, e);
        }
    }
}
