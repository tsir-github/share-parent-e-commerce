package com.share.payment.controller;

import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.security.annotation.InnerAuth;
import com.share.common.security.annotation.RequiresLogin;
import com.share.payment.domain.dto.CreatePaymentDTO;
import com.share.payment.service.IPaymentInfoService;
import com.share.common.security.utils.SecurityUtils;
import com.share.payment.api.RefundRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 付款信息Controller
 *
 * 路径说明（Gateway StripPrefix=1）：
 *   外部请求 /payment/api/v1/payment/create → 转发到 /api/v1/payment/create
 *   Feign 直调 /inner/payment/status/{orderNo}（不经过 Gateway）
 *
 * 接口汇总：
 *   POST /api/v1/payment/create         — C端发起支付
 *   POST /api/v1/payment/callback       — 微信回调（mock 模式下忽略，生产用）
 *   POST /api/v1/payment/mock/callback  — 模拟支付成功（仅 mock 模式可用）
 *   GET  /inner/payment/status/{no}     — Feign 查询支付状态
 *   POST /inner/payment/create          — Feign 创建支付
 *   GET  /inner/payment/{no}            — Feign 查询支付信息
 *
 * 管理端接口已迁移至 {@link PaymentInfoAdminController}。
 */
@Tag(name = "付款信息管理")
@RestController
@RequiredArgsConstructor
public class PaymentInfoController extends BaseController {

    private final IPaymentInfoService paymentInfoService;

    @Operation(summary = "创建支付单")
    @RequiresLogin
    @PostMapping("/api/v1/payment/create")
    public R<Map<String, String>> createPayment(@RequestParam String orderNo,
                                                @RequestParam BigDecimal amount,
                                                @RequestParam String description,
                                                @RequestParam String openid) {
        Long userId = SecurityUtils.getUserId();
        Map<String, String> params = paymentInfoService.createPayment(orderNo, userId, amount, description, openid);
        return R.ok(params);
    }

    @Operation(summary = "微信支付回调")
    @PostMapping("/api/v1/payment/callback")
    public Map<String, String> payCallback(HttpServletRequest request) {
        // 微信网关要求返回 {"code":"SUCCESS","message":"成功"} 格式
        // 不能使用 R<T> 包装，因为微信不识别
        return paymentInfoService.handlePayCallback(request);
    }

    @Operation(summary = "微信退款回调")
    @PostMapping("/api/v1/payment/refund/callback")
    public Map<String, String> refundCallback(HttpServletRequest request) {
        return paymentInfoService.handleRefundCallback(request);
    }

    @Operation(summary = "模拟支付成功（mock 模式）")
    @RequiresLogin
    @PostMapping("/api/v1/payment/mock/callback")
    public R<Void> mockCallback(@RequestParam String orderNo) {
        paymentInfoService.mockPaySuccess(orderNo);
        return R.ok();
    }

    @Operation(summary = "查询支付状态（Feign）")
    @InnerAuth
    @GetMapping("/inner/payment/status/{orderNo}")
    public R<Integer> getPaymentStatus(@PathVariable String orderNo) {
        Integer status = paymentInfoService.getPaymentStatusByOrderNo(orderNo);
        return R.ok(status);
    }

    @Operation(summary = "Feign 创建支付")
    @InnerAuth
    @PostMapping("/inner/payment/create")
    public R<Void> createPaymentInner(@RequestBody @Valid CreatePaymentDTO dto) {
        paymentInfoService.createPayment(dto.getOrderNo(), dto.getUserId(), dto.getAmount(), dto.getDescription(), dto.getOpenid());
        return R.ok();
    }

    @Operation(summary = "Feign 发起退款")
    @InnerAuth
    @PostMapping("/inner/payment/refund")
    public R<Void> refundInner(@RequestBody RefundRequest request) {
        paymentInfoService.refund(request.getOrderNo(), request.getAmount(), request.getReason());
        return R.ok();
    }

    @Operation(summary = "Feign 查询支付信息")
    @InnerAuth
    @GetMapping("/inner/payment/{orderNo}")
    public R<Map<String, Object>> getPaymentByOrderNo(@PathVariable String orderNo) {
        Map<String, Object> result = paymentInfoService.getPaymentMapByOrderNo(orderNo);
        if (result.isEmpty()) {
            return R.fail("支付记录不存在");
        }
        return R.ok(result);
    }

}
