package com.share.order.config;

import com.share.common.core.constant.MqConstants;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.goods.api.RemoteSeckillService;
import com.share.order.api.RemoteOrderInfoService;
import com.share.order.domain.dto.SeckillOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀异步下单消费者
 *
 * 消费 share-goods 投递的秒杀下单消息，异步完成订单落库。
 * 幂等由 share-order 内部 Redisson 分布式锁 + seckill_order 唯一索引保证。
 * 失败后重试 16 次，最终进入死信队列。
 *
 * @author share
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.SECKILL_ORDER_CREATE_TOPIC,
        consumerGroup = "seckill-order-consumer-group",
        maxReconsumeTimes = 16
)
public class SeckillConsumer implements RocketMQListener<SeckillOrderRequest> {

    private final RemoteOrderInfoService remoteOrderInfoService;
    private final RemoteSeckillService remoteSeckillService;

    @Override
    public void onMessage(SeckillOrderRequest request) {
        log.info("消费秒杀下单消息: orderNo={}, activityId={}, userId={}",
                request.getOrderNo(), request.getSeckillActivityId(), request.getUserId());

        R<String> result = remoteOrderInfoService.createSeckillOrder(request, SecurityConstants.INNER);
        if (result.getCode() != 200) {
            // ponytail: 失败归还 Redis 库存，消息重试
            log.error("秒杀订单创建失败（将重试并归还库存）: orderNo={}, msg={}", request.getOrderNo(), result.getMsg());
            try {
                remoteSeckillService.releaseStock(
                        request.getSeckillActivityId(), request.getItems().get(0).getQuantity(),
                        SecurityConstants.INNER);
            } catch (Exception e) {
                log.error("归还秒杀库存失败: activityId={}", request.getSeckillActivityId(), e);
            }
            throw new RuntimeException("秒杀订单创建失败: " + result.getMsg());
        }

        log.info("秒杀订单创建成功: orderNo={}", request.getOrderNo());
    }
}
