package com.share.common.core.utils.uuid;

import java.util.UUID;

/**
 * ID生成器工具类
 *
 * @author share
 */
public class IdUtils
{
    /**
     * 获取随机UUID
     *
     * @return 随机UUID
     */
    public static String randomUUID()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取随机UUID（同 randomUUID，保留作为兼容别名）
     *
     * @return 随机UUID
     */
    public static String fastUUID()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线（同 simpleUUID，保留作为兼容别名）
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUUID()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 雪花算法生成分布式 ID（long 型）
     *
     * @return 分布式 ID
     */
    public static long snowflakeId()
    {
        return SnowflakeIdWorker.nextLongId();
    }

    /**
     * 雪花算法生成分布式 ID（字符串型）
     *
     * @return 分布式 ID 字符串
     */
    public static String snowflakeIdStr()
    {
        return SnowflakeIdWorker.nextIdStr();
    }

    /**
     * 号段模式生成订单号（long 型）
     * <p>
     * 使用前需在模块启动时调用 IdSegmentGenerator.register("order_no", ...) 注册 DB fetcher。
     *
     * @return 订单号
     */
    public static long orderNo()
    {
        return IdSegmentGenerator.nextId("order_no");
    }
}
