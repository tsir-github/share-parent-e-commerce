package com.share.coupon.factory;

import com.share.common.core.domain.R;
import com.share.coupon.api.RemoteCouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 优惠券服务降级处理
 *
 * @author share
 */
@Component
public class RemoteCouponFallbackFactory implements FallbackFactory<RemoteCouponService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteCouponFallbackFactory.class);

    @Override
    public RemoteCouponService create(Throwable throwable) {
        log.error("优惠券服务调用失败: {}", throwable.getMessage());
        return new RemoteCouponService() {
            @Override
            public R<Void> releaseByOrderNo(Map<String, Object> params, String source) {
                log.warn("释放优惠券降级: orderNo={}", params.get("orderNo"));
                return R.ok();
            }

            @Override
            public R<Void> lockForOrder(Map<String, Object> params, String source) {
                log.warn("锁定优惠券降级: couponUserId={}", params.get("couponUserId"));
                return R.fail("优惠券锁定失败，请稍后重试");
            }

            @Override
            public R<Void> releaseForOrder(Map<String, Object> params, String source) {
                log.warn("释放优惠券降级: couponUserId={}", params.get("couponUserId"));
                return R.ok();
            }

            @Override
            public R<Integer> countAvailable(Long userId, String source) {
                log.warn("查询可用优惠券数量降级: userId={}", userId);
                return R.ok(0);
            }

            @Override
            public R<Void> consume(Map<String, Object> params, String source) {
                log.warn("消费优惠券降级: couponUserId={}", params.get("couponUserId"));
                return R.fail("优惠券核销失败");
            }
        };
    }
}
