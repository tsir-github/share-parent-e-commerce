package com.share.order.constant;

/**
 * 售后审核状态常量
 *
 * <pre>
 * after_sale_request.audit_status（char(1)）:
 *   0-待审核    1-商家同意    2-商家拒绝
 *   3-客服介入  4-客服同意退款  5-客服拒绝
 * </pre>
 *
 * @author share
 */
public class AfterSaleStatus
{
    /** 待审核 */
    public static final String PENDING = "0";

    /** 商家同意 */
    public static final String APPROVED = "1";

    /** 商家拒绝 */
    public static final String REJECTED = "2";

    /** 客服介入 */
    public static final String SERVICE_INTERVENTION = "3";

    /** 客服同意退款 */
    public static final String SERVICE_APPROVED = "4";

    /** 客服拒绝 */
    public static final String SERVICE_REJECTED = "5";

    private AfterSaleStatus()
    {
        // 工具类禁止实例化
    }
}
