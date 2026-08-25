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

    /** 商家新订单通知 */
    String ORDER_MERCHANT_NOTIFY_TOPIC = "order-merchant-notify";

    /** 秒杀异步下单 */
    String SECKILL_ORDER_CREATE_TOPIC = "seckill-order-create";

    /** 缓存失效延迟删除（Consumer 收到后删 Redis key，延迟级别3=10s，兜底双删失败） */
    String CACHE_INVALIDATE_TOPIC = "cache-invalidate";
}
