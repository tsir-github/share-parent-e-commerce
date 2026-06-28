package com.share.payment.mq;

import com.share.common.core.constant.MqConstants;
import com.share.payment.domain.dto.PaymentSuccessMessage;
import com.share.payment.domain.dto.RefundSuccessMessage;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付消息 MQ 生产者
 *
 * 封装支付相关 MQ 消息发送逻辑。统一入口便于后续加埋点/重试/事务消息。
 *
 * RocketMQTemplate 为可选注入：当 Nacos 中未配置 rocketmq.name-server 时，
 * 该 Bean 不存在，组件以降级模式运行（仅打日志，不发送 MQ），
 * 不影响 mock 模式下的开发调试。补偿定时任务会在 RocketMQ 恢复后自动重试。
 */
@Slf4j
@Component
public class PaymentMQProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Autowired(required = false)
    public PaymentMQProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 发送支付成功消息（通知订单模块更新状态）
     *
     * @param orderNo       订单号
     * @param transactionId 微信交易号
     * @return true=发送成功, false=发送失败
     */
    public boolean sendPaySuccessMessage(String orderNo, String transactionId) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ 未配置（rocketmq.name-server 缺失），跳过 MQ 发送: orderNo={}", orderNo);
            return false;
        }
        try {
            PaymentSuccessMessage msg = new PaymentSuccessMessage(orderNo, transactionId);
            rocketMQTemplate.convertAndSend(MqConstants.PAYMENT_SUCCESS_TOPIC, msg);
            log.info("支付成功 MQ 已发送: orderNo={}, transactionId={}", orderNo, transactionId);
            return true;
        } catch (Exception e) {
            log.error("支付成功 MQ 发送失败: orderNo={}", orderNo, e);
            return false;
        }
    }

    /**
     * 发送退款成功消息（通知订单模块更新状态）
     *
     * @param orderNo       订单号
     * @param transactionId 微信退款单号
     * @param refundAmount  退款金额
     * @return true=发送成功, false=发送失败
     */
    public boolean sendRefundSuccessMessage(String orderNo, String transactionId, BigDecimal refundAmount) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ 未配置（rocketmq.name-server 缺失），跳过 MQ 发送: orderNo={}", orderNo);
            return false;
        }
        try {
            RefundSuccessMessage msg = new RefundSuccessMessage(orderNo, transactionId, refundAmount);
            rocketMQTemplate.convertAndSend(MqConstants.REFUND_SUCCESS_TOPIC, msg);
            log.info("退款成功 MQ 已发送: orderNo={}, transactionId={}, refundAmount={}", orderNo, transactionId, refundAmount);
            return true;
        } catch (Exception e) {
            log.error("退款成功 MQ 发送失败: orderNo={}", orderNo, e);
            return false;
        }
    }
}
