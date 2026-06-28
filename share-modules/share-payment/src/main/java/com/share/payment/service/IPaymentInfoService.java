package com.share.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.payment.domain.PaymentInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 付款信息Service接口
 *
 * @author share
 */
public interface IPaymentInfoService extends IService<PaymentInfo> {

    /**
     * 创建支付单
     *
     * @param orderNo     订单号
     * @param userId      用户ID
     * @param amount      支付金额
     * @param description 交易描述
     * @param openid      微信 openid（JSAPI 支付需要）
     * @return 前端调起支付参数 {appId, timeStamp, nonceStr, package, paySign}
     */
    Map<String, String> createPayment(String orderNo, Long userId, BigDecimal amount,
                                      String description, String openid);

    /**
     * 处理微信支付异步回调
     * <p>
     * 返回 {@code {"code":"SUCCESS","message":"成功"}} 格式给微信网关，
     * 不包装在 {@code R<T>} 中，因为微信网关不识别 RuoYi 的统一返回格式。
     *
     * @param request HTTP 请求（包含微信回调的请求头和加密 body）
     * @return 微信要求的响应 Map
     */
    Map<String, String> handlePayCallback(HttpServletRequest request);

    /**
     * 处理微信退款异步回调
     *
     * @return 微信要求的响应 Map
     */
    Map<String, String> handleRefundCallback(HttpServletRequest request);

    /**
     * 模拟支付成功（mock 模式专用）
     */
    void mockPaySuccess(String orderNo);

    /**
     * 发起退款
     *
     * @param orderNo 订单号
     * @param amount  退款金额
     * @param reason  退款原因
     */
    void refund(String orderNo, BigDecimal amount, String reason);

    /**
     * 根据订单号查询支付状态
     *
     * @param orderNo 订单号
     * @return paymentStatus（0-未支付 1-已支付 2-已退款 3-退款中）
     */
    Integer getPaymentStatusByOrderNo(String orderNo);

    /**
     * 分页查询支付记录（后台管理）
     *
     * @param query 查询条件（orderNo, paymentStatus, userId）
     * @return 支付记录列表
     */
    List<PaymentInfo> selectPaymentList(PaymentInfo query);

    /**
     * 根据订单号查询支付信息（Feign 用）
     *
     * @param orderNo 订单号
     * @return 支付信息 Map（不返回敏感字段）
     */
    Map<String, Object> getPaymentMapByOrderNo(String orderNo);
}
