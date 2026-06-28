package com.share.common.core.constant;

/**
 * 分类状态常量
 *
 * <pre>
 * category.status（char(1)）:
 *   0-正常  1-禁用
 * </pre>
 *
 * @author share
 */
public class CategoryStatus
{
    /** 正常 */
    public static final String NORMAL = "0";

    /** 禁用 */
    public static final String DISABLED = "1";

    private CategoryStatus()
    {
        // 工具类禁止实例化
    }
}
