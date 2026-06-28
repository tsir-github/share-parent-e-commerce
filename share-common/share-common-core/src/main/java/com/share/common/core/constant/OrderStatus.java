package com.share.common.core.constant;

/**
 * 订单状态常量
 *
 * <pre>
 * 订单三维状态模型：
 *   status（主生命周期）:
 *     0-待支付 1-待发货 2-配送中 3-已完成 4-已取消 5-售后中
 *   payStatus（支付维度）:
 *     0-未支付 1-已支付 2-已退款
 *   deliveryStatus（物流维度）:
 *     0-未配送 1-配送中 2-已送达 3-已确认
 * </pre>
 *
 * @author share
 */
public class OrderStatus
{
    // ==================== 主状态（status） ====================

    /** 待支付 */
    public static final String PENDING_PAY = "0";

    /** 待发货 */
    public static final String PENDING_DELIVERY = "1";

    /** 配送中 */
    public static final String DELIVERING = "2";

    /** 已完成 */
    public static final String COMPLETED = "3";

    /** 已取消 */
    public static final String CANCELLED = "4";

    /** 售后中 */
    public static final String AFTER_SALE = "5";

    // ==================== 支付状态（pay_status） ====================

    /** 未支付 */
    public static final String PAY_UNPAID = "0";

    /** 已支付 */
    public static final String PAY_PAID = "1";

    /** 已退款 */
    public static final String PAY_REFUNDED = "2";

    // ==================== 配送状态（delivery_status） ====================

    /** 未配送 */
    public static final String DELIVERY_UNDELIVERED = "0";

    /** 配送中 */
    public static final String DELIVERY_IN_TRANSIT = "1";

    /** 已送达 */
    public static final String DELIVERY_DELIVERED = "2";

    /** 已确认 */
    public static final String DELIVERY_CONFIRMED = "3";

    // ==================== 订单类型（order_type） ====================

    /** 普通订单 */
    public static final String ORDER_TYPE_NORMAL = "0";

    /** 秒杀订单 */
    public static final String ORDER_TYPE_FLASH = "1";

    /** 拼团订单 */
    public static final String ORDER_TYPE_GROUP = "2";

    // ==================== 关闭类型（close_type） ====================

    /** 用户取消 */
    public static final String CLOSE_TYPE_USER = "1";

    /** 超时取消 */
    public static final String CLOSE_TYPE_TIMEOUT = "2";

    /** 商家取消 */
    public static final String CLOSE_TYPE_SHOP = "3";

    /** 客服取消 */
    public static final String CLOSE_TYPE_SERVICE = "4";

    private OrderStatus()
    {
        // 工具类禁止实例化
    }
}
