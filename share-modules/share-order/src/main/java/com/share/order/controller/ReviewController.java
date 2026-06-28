package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.dto.SubmitReviewDTO;
import com.share.order.domain.vo.OrderReviewVO;
import com.share.order.domain.vo.PendingReviewVO;
import com.share.order.service.IOrderReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端评价 Controller
 *
 * @author share
 */
@Tag(name = "C端评价")
@RestController
@RequestMapping("/api/v1/order/review")
@RequiredArgsConstructor
public class ReviewController {

    private final IOrderReviewService orderReviewService;

    @Operation(summary = "提交评价")
    @RequiresLogin
    @PostMapping
    public R<Long> submitReview(@Valid @RequestBody SubmitReviewDTO dto) {
        Long userId = SecurityUtils.getUserId();
        Long reviewId = orderReviewService.submitReview(dto.getOrderItemId(), userId, dto.getRating(), dto.getContent());
        return R.ok(reviewId);
    }

    @Operation(summary = "查询订单项评价")
    @RequiresLogin
    @GetMapping("/item/{orderItemId}")
    public R<OrderReviewVO> getReview(@PathVariable Long orderItemId) {
        OrderReviewVO vo = orderReviewService.getReviewByOrderItemId(orderItemId);
        if (vo == null) {
            return R.ok(null);
        }
        return R.ok(vo);
    }

    @Operation(summary = "待评价列表")
    @RequiresLogin
    @GetMapping("/pending")
    public R<List<PendingReviewVO>> pendingReviews() {
        Long userId = SecurityUtils.getUserId();
        return R.ok(orderReviewService.getPendingReviews(userId));
    }

    @Operation(summary = "商品评价列表（公开）")
    @GetMapping("/product/{productId}")
    public R<List<OrderReviewVO>> productReviews(@PathVariable Long productId) {
        return R.ok(orderReviewService.getProductReviews(productId));
    }
}
