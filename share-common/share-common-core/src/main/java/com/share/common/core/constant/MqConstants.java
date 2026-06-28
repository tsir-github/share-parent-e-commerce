package com.share.common.core.constant;

/**
 * MQ 主题常量
 *
 * RocketMQ broker 已开启 autoCreateTopicEnable=true，无需手动创建 topic。
 */
public interface MqConstants {

    /** 支付成功 → 订单状态变更 */
    String PAYMENT_SUCCESS_TOPIC = "order-pay-success";

    /** 订单超时取消（延迟消息，30min） */
    String ORDER_TIMEOUT_CANCEL_TOPIC = "order-timeout-cancel";

    /** 订单自动确认收货（延迟消息，7天） */
    String ORDER_AUTO_CONFIRM_TOPIC = "order-auto-confirm";

    /** 退款成功 → 订单状态变更 */
    String REFUND_SUCCESS_TOPIC = "order-refund-success";
}
