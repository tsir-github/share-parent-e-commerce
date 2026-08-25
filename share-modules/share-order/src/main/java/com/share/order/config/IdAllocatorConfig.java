package com.share.order.config;

import com.share.common.core.utils.uuid.IdSegmentGenerator;
import com.share.common.core.utils.uuid.IdSegmentGenerator.IdSegment;
import com.share.order.mapper.IdAllocatorMapper;
import com.share.order.mapper.IdAllocatorMapper.IdAllocatorRow;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 号段 ID 分配器初始化 —— 注册 order_no 号段获取器到 IdSegmentGenerator
 * <p>
 * 每次 DB 交互取 step=500 的号段存内存，耗完再取，减少 DB 压力。
 *
 * @author share
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdAllocatorConfig {

    private static final int STEP = 500;
    private static final int MAX_RETRY = 3;

    private final IdAllocatorMapper mapper;

    @PostConstruct
    public void init() {
        IdSegmentGenerator.register("order_no", this::fetchSegment);
        log.info("order_no 号段获取器已注册");
    }

    private IdSegment fetchSegment() {
        for (int i = 0; i < MAX_RETRY; i++) {
            IdAllocatorRow row = mapper.selectForUpdate("order_no");
            if (row == null) {
                throw new IllegalStateException("id_allocator 中未找到 order_no 记录");
            }
            int affected = mapper.allocate("order_no", STEP, row.getVersion());
            if (affected > 0) {
                long newMin = row.getMaxId() + 1;
                long newMax = row.getMaxId() + STEP;
                log.debug("取号段成功: [{}, {}]", newMin, newMax);
                return new IdSegment(newMin, newMax);
            }
            // 乐观锁冲突，短暂等待后重试
            try { Thread.sleep(10L * (i + 1)); } catch (InterruptedException ignored) {}
        }
        throw new IllegalStateException("取号段失败，重试 " + MAX_RETRY + " 次后仍未成功");
    }
}
