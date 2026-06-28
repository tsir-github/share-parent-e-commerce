package com.share.order.config;

import com.share.common.core.utils.uuid.IdSegmentGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 号段模式发号器初始化
 * <p>
 * 模块启动时注册 order_no 的 DB fetcher，后续 IdSegmentGenerator.nextId("order_no") 自动走号段模式。
 *
 * @author share
 */
@Component
public class IdAllocatorInitializer {

    private static final Logger log = LoggerFactory.getLogger(IdAllocatorInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public IdAllocatorInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        IdSegmentGenerator.register("order_no", () -> {
            // 自旋重试乐观锁
            while (true) {
                Map<String, Object> row = jdbcTemplate.queryForMap(
                        "SELECT max_id, step, version FROM id_allocator WHERE biz_tag = ?", "order_no");

                long oldMax = ((Number) row.get("max_id")).longValue();
                int step = ((Number) row.get("step")).intValue();
                long oldVersion = ((Number) row.get("version")).longValue();

                int affected = jdbcTemplate.update(
                        "UPDATE id_allocator SET max_id = max_id + ?, version = version + 1 " +
                                "WHERE biz_tag = ? AND version = ?",
                        step, "order_no", oldVersion);

                if (affected > 0) {
                    long minId = oldMax + 1;
                    long maxId = oldMax + step;
                    log.info("号段分配 [order_no] min={}, max={}", minId, maxId);
                    return new IdSegmentGenerator.IdSegment(minId, maxId);
                }
                // 乐观锁冲突，重试
            }
        });
        log.info("IdSegmentGenerator: order_no 号段模式已注册");
    }
}
