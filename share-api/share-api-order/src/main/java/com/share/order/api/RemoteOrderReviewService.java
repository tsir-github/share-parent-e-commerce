package com.share.order.api;

import com.share.common.core.domain.R;
import com.share.order.domain.vo.ReviewStatsDTO;
import com.share.order.domain.vo.OrderReviewVO;
import com.share.order.factory.RemoteOrderReviewFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 评价服务 Feign 接口
 *
 * @author share
 */
@FeignClient(contextId = "remoteOrderReviewService",
        value = "share-order",
        fallbackFactory = RemoteOrderReviewFallbackFactory.class)
public interface RemoteOrderReviewService {

    @GetMapping("/inner/review/stats/{productId}")
    R<ReviewStatsDTO> getReviewStats(@PathVariable("productId") Long productId);

    @PostMapping("/inner/review/stats/batch")
    R<Map<Long, ReviewStatsDTO>> getReviewStatsBatch(@RequestBody List<Long> productIds);

    @GetMapping("/inner/review/product/{productId}")
    R<List<OrderReviewVO>> getProductReviews(@PathVariable("productId") Long productId);
}
