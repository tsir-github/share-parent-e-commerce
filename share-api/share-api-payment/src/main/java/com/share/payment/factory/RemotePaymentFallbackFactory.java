package com.share.payment.factory;

import com.share.common.core.domain.R;
import com.share.payment.api.RefundRequest;
import com.share.payment.api.RemotePaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付服务降级处理
 *
 * @author share
 */
@Component
public class RemotePaymentFallbackFactory implements FallbackFactory<RemotePaymentService> {

    private static final Logger log = LoggerFactory.getLogger(RemotePaymentFallbackFactory.class);

    @Override
    public RemotePaymentService create(Throwable throwable) {
        log.error("支付服务调用失败: {}", throwable.getMessage());
        return new RemotePaymentService() {
            @Override
            public R<Void> createPayment(Map<String, Object> params, String source) {
                return R.fail("创建支付失败:" + throwable.getMessage());
            }

            @Override
            public R<Map<String, Object>> getPaymentByOrderNo(String orderNo, String source) {
                return R.fail("查询支付失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> getPaymentStatus(String orderNo, String source) {
                return R.fail("查询支付状态失败:" + throwable.getMessage());
            }

            @Override
            public R<Void> refund(RefundRequest request, String source) {
                return R.fail("退款失败:" + throwable.getMessage());
            }
        };
    }
}
