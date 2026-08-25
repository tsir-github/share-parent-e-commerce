package com.share.coupon.constant;

/**
 * 优惠券状态常量
 *
 * <pre>
 * coupon_template.type (char(1)):
 *   0-满减券  1-折扣券  2-无门槛券
 *
 * coupon_template.status (char(1)):
 *   0-未启用  1-已启用
 *
 * coupon_user.status (char(1)):
 *   0-未使用  1-已使用  2-已过期  3-已消费
 * </pre>
 *
 * @author share
 */
public class CouponStatus
{
    // ==================== 优惠券类型（coupon_template.type） ====================

    /** 满减券 */
    public static final String TYPE_FULL_REDUCTION = "0";

    /** 折扣券 */
    public static final String TYPE_DISCOUNT = "1";

    /** 无门槛券 */
    public static final String TYPE_NO_THRESHOLD = "2";

    // ==================== 用户优惠券状态（coupon_user.status） ====================

    /** 未使用 */
    public static final String USER_UNUSED = "0";

    /** 已使用 */
    public static final String USER_USED = "1";

    /** 已过期 */
    public static final String USER_EXPIRED = "2";

    /** 已消费（订单已完成） */
    public static final String USER_CONSUMED = "3";

    // ==================== 优惠券模板状态（coupon_template.status） ====================

    /** 未启用 */
    public static final String TEMPLATE_DISABLED = "0";

    /** 已启用 */
    public static final String TEMPLATE_ENABLED = "1";

    private CouponStatus()
    {
    }
}
