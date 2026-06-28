package com.share.common.core.constant;

/**
 * 支付记录状态常量
 *
 * 对应 payment_info.payment_status 字段（tinyint）
 * 注意：与 OrderStatus.PAY_xxx（String 类型，对应 order_info.pay_status）不同，
 * 此处为 Integer 类型，用于支付记录表。
 *
 * @author share
 */
public class PaymentStatus
{
    /** 未支付 */
    public static final Integer UNPAID = 0;

    /** 已支付 */
    public static final Integer PAID = 1;

    /** 已退款 */
    public static final Integer REFUNDED = 2;

    /** 退款中 */
    public static final Integer REFUNDING = 3;

    private PaymentStatus()
    {
        // 工具类禁止实例化
    }
}
