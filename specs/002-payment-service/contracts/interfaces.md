# Service Interface: IPaymentInfoService

`java
package com.share.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.payment.domain.PaymentInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 付款信息Service接口
 */
public interface IPaymentInfoService extends IService<PaymentInfo> {

    /**
     * 创建支付单
     * @return 前端调起支付参数 (appId, timeStamp, nonceStr, package, paySign)
     */
    Map<String, String> createPayment(String orderNo, Long userId, BigDecimal amount,
                                      String description, String openid);

    /**
     * 处理微信支付异步回调
     * @return 微信要求的响应 XML/JSON
     */
    String handlePayCallback(HttpServletRequest request);

    /**
     * 处理微信退款异步回调
     */
    String handleRefundCallback(HttpServletRequest request);

    /**
     * 模拟支付成功（mock 模式）
     */
    void mockPaySuccess(String orderNo);

    /**
     * 发起退款
     */
    void refund(String orderNo, BigDecimal amount, String reason);

    /**
     * 根据订单号查询支付状态
     */
    Integer getPaymentStatusByOrderNo(String orderNo);
}
`

---

# Feign Contract: share-order → share-payment

## RemotePaymentService（已存在，路径需修正）

`java
// 当前 API 路径（share-api-payment）
@FeignClient(contextId = "remotePaymentService",
             value = ServiceNameConstants.PAYMENT_SERVICE,
             fallbackFactory = RemotePaymentFallbackFactory.class)
public interface RemotePaymentService {
    // POST /inner/payment/create  ← 需在 Controller 中实现
    R<Void> createPayment(@RequestBody Map<String, Object> params,
                          @RequestHeader(FROM_SOURCE) String source);

    // GET /inner/payment/{orderNo}  ← 需在 Controller 中实现
    R<Map<String, Object>> getPaymentByOrderNo(@PathVariable String orderNo,
                                               @RequestHeader(FROM_SOURCE) String source);
}
`

---

# MQ Contract: Payment → Order

## Topic: \order-pay-success\

**Producer**: share-payment → 支付成功时发送  
**Consumer**: share-order → 收到后更新订单状态  
**Message Type**: PaymentSuccessMessage { orderNo, transactionId }  
**Reliability**: at-least-once (消费者幂等)  
**Compensation**: 定时任务扫描 payment_info 中 status=1 但未发送成功的记录
