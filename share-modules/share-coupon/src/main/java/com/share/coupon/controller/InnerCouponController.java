package com.share.coupon.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.coupon.service.ICouponUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 优惠券内部Feign接口
 *
 * @author share
 */
@Tag(name = "优惠券内部接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/inner/coupon")
public class InnerCouponController {

    private final ICouponUserService couponUserService;

    @Operation(summary = "锁定优惠券（下单占用）")
    @InnerAuth
    @PostMapping("/lock")
    public R<Void> lock(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long couponUserId = Long.valueOf(params.get("couponUserId").toString());
        String orderNo = params.get("orderNo").toString();
        couponUserService.lockForOrder(userId, couponUserId, orderNo);
        return R.ok();
    }

    @Operation(summary = "释放优惠券（取消订单回退）")
    @InnerAuth
    @PostMapping("/release")
    public R<Void> release(@RequestBody Map<String, Object> params) {
        Long couponUserId = Long.valueOf(params.get("couponUserId").toString());
        String orderNo = params.get("orderNo").toString();
        couponUserService.releaseForOrder(couponUserId, orderNo);
        return R.ok();
    }

    @Operation(summary = "批量释放订单关联的优惠券（取消订单回退）")
    @InnerAuth
    @PostMapping("/releaseByOrderNo")
    public R<Void> releaseByOrderNo(@RequestBody Map<String, Object> params) {
        String orderNo = params.get("orderNo").toString();
        couponUserService.releaseByOrderNo(orderNo);
        return R.ok();
    }

    @Operation(summary = "查询用户可用优惠券数量")
    @InnerAuth
    @GetMapping("/count/{userId}")
    public R<Integer> countAvailable(@PathVariable Long userId) {
        long count = couponUserService.countAvailableByUserId(userId);
        return R.ok((int) count);
    }

    @Operation(summary = "消费优惠券（订单完成后标记已使用）")
    @InnerAuth
    @PostMapping("/consume")
    public R<Void> consume(@RequestBody Map<String, Object> params) {
        Long couponUserId = Long.valueOf(params.get("couponUserId").toString());
        couponUserService.consumeCoupon(couponUserId);
        return R.ok();
    }
}
