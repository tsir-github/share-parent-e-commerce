package com.share.order.factory;

import com.share.common.core.domain.R;
import com.share.order.api.RemoteOrderReviewService;
import com.share.order.domain.vo.ReviewStatsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 评价服务降级处理
 *
 * @author share
 */
@Component
public class RemoteOrderReviewFallbackFactory implements FallbackFactory<RemoteOrderReviewService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteOrderReviewFallbackFactory.class);

    @Override
    public RemoteOrderReviewService create(Throwable throwable) {
        log.error("评价服务调用失败: {}", throwable.getMessage(), throwable);
        return new RemoteOrderReviewService() {
            @Override
            public R<ReviewStatsDTO> getReviewStats(Long productId) {
                return R.fail("获取评价统计失败:" + throwable.getMessage());
            }

            @Override
            public R<Map<Long, ReviewStatsDTO>> getReviewStatsBatch(List<Long> productIds) {
                return R.fail("批量获取评价统计失败:" + throwable.getMessage());
            }
        };
    }
}
