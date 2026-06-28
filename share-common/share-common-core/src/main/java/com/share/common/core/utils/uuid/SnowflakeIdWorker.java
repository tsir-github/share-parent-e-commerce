package com.share.common.core.utils.uuid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 结构：0(1bit) | 时间戳(41bit) | 机房ID(5bit) | 机器ID(5bit) | 序列号(12bit)
 * 整体按 64 位 long 存储，可用至 2089 年。
 * </p>
 *
 * @author share
 */
public class SnowflakeIdWorker {

    // ============================== 常量 ====================================
    /** 开始时间戳（2025-01-01） */
    private final long twepoch = 1735689600000L;

    /** 机器 ID 所占位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心 ID 所占位数 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 支持的最大机器 ID，结果 31 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 支持的最大数据中心 ID，结果 31 */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 序列号所占位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器 ID 左移位数（12） */
    private final long workerIdShift = SEQUENCE_BITS;

    /** 数据中心 ID 左移位数（12+5=17） */
    private final long datacenterIdShift = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数（12+5+5=22） */
    private final long timestampLeftShift = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /** 生成序列的掩码（4095） */
    private final long sequenceMask = ~(-1L << SEQUENCE_BITS);

    // ============================== 成员变量 =================================

    /** 工作机器 ID */
    private long workerId;

    /** 数据中心 ID */
    private long datacenterId;

    /** 毫秒内序列 */
    private long sequence = 0L;

    /** 上次生成 ID 的时间戳 */
    private long lastTimestamp = -1L;

    // ============================== 构造 =====================================

    private static volatile SnowflakeIdWorker instance;

    private SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获取单例，自动从本地 MAC 地址解析 workerId / datacenterId
     */
    public static SnowflakeIdWorker getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdWorker.class) {
                if (instance == null) {
                    long[] ids = getWorkerAndDatacenterId();
                    instance = new SnowflakeIdWorker(ids[0], ids[1]);
                }
            }
        }
        return instance;
    }

    // ============================== 公开方法 =================================

    /**
     * 获取下一个 ID（线程安全）
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 时钟回拨，抛异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                            lastTimestamp - timestamp));
        }

        // 同一毫秒内，序列号递增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 当前毫秒序列用完，自旋等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    /**
     * 获取下一个 ID（字符串形式）
     */
    public static String nextIdStr() {
        return String.valueOf(getInstance().nextId());
    }

    /**
     * 获取下一个 ID（long 形式）
     */
    public static long nextLongId() {
        return getInstance().nextId();
    }

    // ============================== 内部方法 =================================

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 根据本地 MAC 地址生成稳定的 workerId / datacenterId（0~31）
     */
    private static long[] getWorkerAndDatacenterId() {
        long workerId = 0L;
        long datacenterId = 0L;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                        continue;
                    }
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null && mac.length > 0) {
                        long hash = 0L;
                        for (byte b : mac) {
                            hash = hash * 31 + (b & 0xFF);
                        }
                        workerId = Math.abs(hash) % (MAX_WORKER_ID + 1);
                        datacenterId = (Math.abs(hash) >> 8) % (MAX_DATACENTER_ID + 1);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 默认值
            workerId = 1L;
            datacenterId = 1L;
        }
        return new long[]{workerId, datacenterId};
    }
}
