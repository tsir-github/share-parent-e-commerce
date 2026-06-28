package com.share.payment.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.domain.R;
import com.share.payment.factory.RemotePaymentFallbackFactory;
import com.share.payment.api.RefundRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 支付服务远程调用
 *
 * @author share
 */
@FeignClient(contextId = "remotePaymentService",
        value = ServiceNameConstants.PAYMENT_SERVICE,
        fallbackFactory = RemotePaymentFallbackFactory.class)
public interface RemotePaymentService {

    /**
     * 创建支付
     */
    @PostMapping("/inner/payment/create")
    R<Void> createPayment(@RequestBody Map<String, Object> params,
                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据订单号查询支付
     */
    @GetMapping("/inner/payment/{orderNo}")
    R<Map<String, Object>> getPaymentByOrderNo(@PathVariable("orderNo") String orderNo,
                                               @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据订单号查询支付状态
     */
    @GetMapping("/inner/payment/status/{orderNo}")
    R<Integer> getPaymentStatus(@PathVariable("orderNo") String orderNo,
                                @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 发起退款（Feign 调用）
     */
    @PostMapping("/inner/payment/refund")
    R<Void> refund(@RequestBody RefundRequest request,
                   @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
