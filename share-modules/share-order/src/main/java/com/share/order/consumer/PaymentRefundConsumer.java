package com.share.order.consumer;

import com.share.common.core.constant.MqConstants;
import com.share.order.service.IOrderInfoService;
import com.share.payment.domain.dto.RefundSuccessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 退款成功消息消费者
 *
 * 监听 order-refund-success 主题，收到退款成功通知后更新订单状态。
 * 幂等：OrderInfoServiceImpl.processRefundSuccess 内部已做 payStatus 校验。
 * 异常不重抛：补偿定时任务会兜底处理未同步记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.REFUND_SUCCESS_TOPIC,
        consumerGroup = "share-order-refund-consumer"
)
public class PaymentRefundConsumer implements RocketMQListener<RefundSuccessMessage> {

    private final IOrderInfoService orderInfoService;

    @Override
    public void onMessage(RefundSuccessMessage message) {
        log.info("收到退款成功消息: orderNo={}, transactionId={}, refundAmount={}",
                message.getOrderNo(), message.getTransactionId(), message.getRefundAmount());
        try {
            orderInfoService.processRefundSuccess(
                    message.getOrderNo(), message.getTransactionId(), message.getRefundAmount());
        } catch (Exception e) {
            log.error("处理退款成功消息异常: orderNo={}", message.getOrderNo(), e);
            // 不抛异常——补偿定时任务会兜底处理
        }
    }
}
