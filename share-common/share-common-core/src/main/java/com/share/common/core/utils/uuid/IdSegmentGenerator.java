package com.share.common.core.utils.uuid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 号段模式 ID 生成器（Leaf segment 简化版）
 * <p>
 * 用法：<pre>
 * // ① 在模块启动时注册 DB fetcher
 * IdSegmentGenerator.register("order_no", () -> {
 *     // UPDATE id_allocator SET max_id = max_id + step, version = version + 1
 *     // WHERE biz_tag = 'order_no' AND version = ?   (乐观锁重试)
 *     // return new IdSegment(minId, maxId);
 * });
 *
 * // ② 获取 ID
 * long id = IdSegmentGenerator.nextId("order_no");
 * </pre>
 *
 * @author share
 */
public class IdSegmentGenerator {

    /** 号段缓存 key=bizTag, value=Segment */
    private static final Map<String, Segment> CACHE = new ConcurrentHashMap<>();
    /** DB 获取器注册表 */
    private static final Map<String, Supplier<IdSegment>> FETCHERS = new ConcurrentHashMap<>();

    private IdSegmentGenerator() {
    }

    /**
     * 注册号段获取器（通常在各模块 @PostConstruct 中调用）
     *
     * @param bizTag  业务标识，如 "order_no"
     * @param fetcher 从 DB 获取新号段的回调 —— 内部需要自旋重试乐观锁
     */
    public static void register(String bizTag, Supplier<IdSegment> fetcher) {
        FETCHERS.put(bizTag, fetcher);
    }

    /**
     * 获取下一个 ID
     *
     * @param bizTag 业务标识
     * @return 唯一 ID
     */
    public static long nextId(String bizTag) {
        Segment seg = CACHE.get(bizTag);
        long id;
        if (seg == null || (id = seg.nextId()) < 0) {
            synchronized (IdSegmentGenerator.class) {
                seg = CACHE.get(bizTag);
                if (seg == null || (id = seg.nextId()) < 0) {
                    Supplier<IdSegment> fetcher = FETCHERS.get(bizTag);
                    if (fetcher == null) {
                        throw new IllegalStateException("IdSegmentGenerator: 未注册 bizTag=" + bizTag);
                    }
                    IdSegment newSeg = fetcher.get();
                    seg = new Segment(newSeg.getMinId(), newSeg.getMaxId());
                    CACHE.put(bizTag, seg);
                    id = seg.nextId();
                }
            }
        }
        return id;
    }

    // ========== 内部类 ==========

    /**
     * 内存号段（线程安全）
     */
    static class Segment {
        private final AtomicLong current;
        private final long maxId;

        Segment(long minId, long maxId) {
            this.current = new AtomicLong(minId);
            this.maxId = maxId;
        }

        long nextId() {
            long id = current.getAndIncrement();
            if (id > maxId) {
                return -1; // 号段耗尽，通知调用方去 DB 取新号段
            }
            return id;
        }
    }

    /**
     * 从 DB 取回的一段连续 ID 区间
     */
    public static class IdSegment {
        private final long minId;
        private final long maxId;

        public IdSegment(long minId, long maxId) {
            this.minId = minId;
            this.maxId = maxId;
        }

        public long getMinId() {
            return minId;
        }

        public long getMaxId() {
            return maxId;
        }
    }
}
