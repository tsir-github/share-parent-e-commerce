package com.share.order.consumer;

import com.share.common.core.constant.MqConstants;
import com.share.order.service.IOrderInfoService;
import com.share.payment.domain.dto.PaymentSuccessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 支付成功消息消费者
 *
 * 监听 order-pay-success 主题，收到支付成功通知后更新订单状态。
 * 幂等：OrderInfoServiceImpl.processPaySuccess 内部已做状态校验和乐观锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.PAYMENT_SUCCESS_TOPIC,
        consumerGroup = "share-order-pay-consumer"
)
public class PaymentSuccessConsumer implements RocketMQListener<PaymentSuccessMessage> {

    private final IOrderInfoService orderInfoService;

    @Override
    public void onMessage(PaymentSuccessMessage message) {
        log.info("收到支付成功消息: orderNo={}, transactionId={}",
                message.getOrderNo(), message.getTransactionId());
        orderInfoService.processPaySuccess(message.getOrderNo(), message.getTransactionId());
        // ponytail: processPaySuccess 异常直接抛出 → RocketMQ 自动重试（默认16次）
        // 幂等保护在 processPaySuccess 内部（订单状态校验），重复消费安全
    }
}
