package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.order.domain.vo.ReviewStatsDTO;
import com.share.order.domain.vo.OrderReviewVO;
import com.share.order.service.IOrderReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价内部接口（Feign 调用）
 *
 * @author share
 */
@Tag(name = "评价内部接口")
@RestController
@RequestMapping("/inner/review")
@RequiredArgsConstructor
public class InnerOrderReviewController {

    private final IOrderReviewService orderReviewService;

    @Operation(summary = "查询商品评价统计（内部）")
    @InnerAuth
    @GetMapping("/stats/{productId}")
    public R<ReviewStatsDTO> getReviewStats(@PathVariable Long productId) {
        return R.ok(orderReviewService.getReviewStats(productId));
    }

    @Operation(summary = "批量查询商品评价统计（内部）")
    @InnerAuth
    @PostMapping("/stats/batch")
    public R<Map<Long, ReviewStatsDTO>> getReviewStatsBatch(@RequestBody List<Long> productIds) {
        return R.ok(orderReviewService.getReviewStatsBatch(productIds));
    }

    @Operation(summary = "商品评价列表（内部Feign）")
    @InnerAuth
    @GetMapping("/product/{productId}")
    public R<List<OrderReviewVO>> getProductReviews(@PathVariable Long productId) {
        return R.ok(orderReviewService.getProductReviews(productId));
    }
}
