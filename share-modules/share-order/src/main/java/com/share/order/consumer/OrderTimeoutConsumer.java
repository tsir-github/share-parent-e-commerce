package com.share.order.consumer;

import com.share.common.core.constant.MqConstants;
import com.share.common.core.constant.OrderStatus;
import com.share.order.service.IOrderInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消消费者
 *
 * 监听 order-timeout-cancel 主题（延迟消息，30min），
 * 收到后检查订单是否仍为待支付状态，是则自动取消。
 * 幂等：cancelOrder 内部通过 transition() 校验状态 + 乐观锁 version 保证。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.ORDER_TIMEOUT_CANCEL_TOPIC,
        consumerGroup = "share-order-timeout-consumer"
)
public class OrderTimeoutConsumer implements RocketMQListener<String> {

    private final IOrderInfoService orderInfoService;

    @Override
    public void onMessage(String orderNo) {
        log.info("收到订单超时取消消息: orderNo={}", orderNo);
        orderInfoService.cancelOrder(
                orderNo,
                OrderStatus.CLOSE_TYPE_TIMEOUT,
                "超时未支付自动取消"
        );
        // cancelOrder 内部校验 status（transition 状态机），非待支付状态跳过，幂等安全
        // 异常直接抛出 → RocketMQ 自动重试
    }
}
