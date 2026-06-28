package com.share.common.core.constant;

/**
 * 商品 SKU 状态常量
 *
 * <pre>
 * product_sku.status（char(1)）:
 *   0-启用  1-禁用
 * </pre>
 *
 * @author share
 */
public class ProductSkuStatus
{
    /** 启用 */
    public static final String ENABLED = "0";

    /** 禁用 */
    public static final String DISABLED = "1";

    private ProductSkuStatus()
    {
        // 工具类禁止实例化
    }
}
