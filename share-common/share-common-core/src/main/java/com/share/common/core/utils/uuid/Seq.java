package com.share.common.core.utils.uuid;

/**
 * 序列号生成器
 *
 * 用于生成全局唯一订单号/序列号
 *
 * @author share
 */
public class Seq {

    /**
     * 获取全局唯一 ID（字符串型）
     *
     * @return 唯一 ID
     */
    public static String getId() {
        return SnowflakeIdWorker.nextIdStr();
    }

    private Seq() {
        // 工具类禁止实例化
    }
}
