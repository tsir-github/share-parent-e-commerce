package com.share.coupon.constant;

/**
 * 优惠券状态常量
 *
 * <pre>
 * coupon_user.status（char(1)）:
 *   0-未使用  1-已锁定（已下单）  2-已过期  3-已消费
 *
 * coupon_template.status（char(1)）:
 *   0-已禁用  1-已启用
 * </pre>
 *
 * @author share
 */
public class CouponStatus
{
    // ==================== 用户优惠券状态（coupon_user.status） ====================

    /** 未使用 */
    public static final String USER_UNUSED = "0";

    /** 已锁定（已下单占用） */
    public static final String USER_USED = "1";

    /** 已过期 */
    public static final String USER_EXPIRED = "2";

    /** 已消费（订单已完成） */
    public static final String USER_CONSUMED = "3";

    // ==================== 优惠券模板状态（coupon_template.status） ====================

    /** 已禁用 */
    public static final String TEMPLATE_DISABLED = "0";

    /** 已启用 */
    public static final String TEMPLATE_ENABLED = "1";

    private CouponStatus()
    {
        // 工具类禁止实例化
    }
}
