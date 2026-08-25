package com.share.common.core.utils.uuid;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 序列号生成器
 *
 * 用于生成全局唯一订单号/序列号
 *
 * @author share
 */
public class Seq {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMdd");

    /**
     * 获取全局唯一 ID（Snowflake，字符串型）
     *
     * @return 唯一 ID
     */
    public static String getId() {
        return SnowflakeIdWorker.nextIdStr();
    }

    /**
     * 获取订单号（日期 + 号段模式）
     * <p>
     * 格式：yyyyMMdd + 6 位序号，例 "20260702000001"
     * <p>
     * 原理：通过 {@link IdSegmentGenerator} 从 DB 号段表分段取号，
     * 每次取 step=500 个号缓存在内存中，耗完再取，避免每次下单都查 DB。
     *
     * @return 订单号
     */
    public static String nextOrderNo() {
        String datePart = DATE_FMT.format(new Date());
        long seq = IdSegmentGenerator.nextId("order_no");
        return datePart + String.format("%06d", seq);
    }

    private Seq() {
        // 工具类禁止实例化
    }
}
