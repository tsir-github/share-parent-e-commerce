package com.share.coupon.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;
import com.share.coupon.factory.RemoteCouponFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 优惠券服务远程调用
 *
 * @author share
 */
@FeignClient(contextId = "remoteCouponService",
        value = ServiceNameConstants.COUPON_SERVICE,
        fallbackFactory = RemoteCouponFallbackFactory.class)
public interface RemoteCouponService {

    /**
     * 释放订单关联的所有优惠券（取消订单回退）
     */
    @PostMapping("/inner/coupon/releaseByOrderNo")
    R<Void> releaseByOrderNo(@RequestBody Map<String, Object> params,
                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 锁定优惠券（下单时占用）
     */
    @PostMapping("/inner/coupon/lock")
    R<Void> lockForOrder(@RequestBody Map<String, Object> params,
                         @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 释放优惠券（取消订单时回退）
     */
    @PostMapping("/inner/coupon/release")
    R<Void> releaseForOrder(@RequestBody Map<String, Object> params,
                            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 查询用户可用优惠券数量
     */
    @GetMapping("/inner/coupon/count/{userId}")
    R<Integer> countAvailable(@PathVariable("userId") Long userId,
                              @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 消费优惠券（订单完成后标记已使用）
     */
    @PostMapping("/inner/coupon/consume")
    R<Void> consume(@RequestBody Map<String, Object> params,
                    @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 查询优惠券详情（含模板折扣信息，下单计算折扣用）
     */
    @PostMapping("/inner/coupon/detail")
    R<Map<String, Object>> getCouponDetail(@RequestBody Map<String, Object> params,
                                            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
