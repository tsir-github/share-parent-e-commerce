package com.share.common.core.constant;

/**
 * 商品 SPU 状态常量
 *
 * <pre>
 * product.status（char(1)）:
 *   0-上架  1-下架
 * </pre>
 *
 * @author share
 */
public class ProductStatus
{
    /** 上架 */
    public static final String LISTED = "0";

    /** 下架 */
    public static final String UNLISTED = "1";

    private ProductStatus()
    {
        // 工具类禁止实例化
    }
}
