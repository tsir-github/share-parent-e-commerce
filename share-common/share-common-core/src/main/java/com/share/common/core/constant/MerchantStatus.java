package com.share.common.core.constant;

/**
 * 商家状态常量
 *
 * <pre>
 * merchant_info.status（char(1)）:
 *   0-待审核  1-已启用  2-已关闭
 *
 * merchant_user.status（char(1)）:
 *   0-正常  1-停用
 * </pre>
 *
 * @author share
 */
public class MerchantStatus
{
    // ==================== 商家信息状态（merchant_info.status） ====================

    /** 待审核 */
    public static final String PENDING_AUDIT = "0";

    /** 已启用 */
    public static final String ENABLED = "1";

    /** 已关闭 */
    public static final String CLOSED = "2";

    // ==================== 商家用户状态（merchant_user.status） ====================

    /** 正常 */
    public static final String USER_NORMAL = "0";

    /** 停用 */
    public static final String USER_DISABLE = "1";

    private MerchantStatus()
    {
        // 工具类禁止实例化
    }
}
