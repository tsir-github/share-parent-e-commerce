package com.share.payment.controller;

import com.share.common.redis.service.IdempotentService;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.payment.domain.PaymentInfo;
import com.share.payment.service.IPaymentInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 支付管理（管理员端）
 *
 * <p>使用 {@code /payment} 路径前缀，与 {@link PaymentInfoController} 的
 * {@code /api/v1/payment/**} 及 {@code /inner/payment/**} 不冲突。</p>
 *
 * @author share
 */
@Tag(name = "支付管理（管理员端）")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentInfoAdminController extends BaseController {

    private final IPaymentInfoService paymentInfoService;
    private final IdempotentService idempotentService;

    /**
     * 查询支付记录列表
     */
    @Operation(summary = "查询支付记录列表")
    @RequiresPermissions("payment:payment:list")
    @GetMapping("/list")
    public TableDataInfo list(PaymentInfo paymentInfo) {
        startPage();
        List<PaymentInfo> list = paymentInfoService.selectPaymentList(paymentInfo);
        return getDataTable(list);
    }

    /**
     * 获取支付记录详细信息
     */
    @Operation(summary = "获取支付记录详细信息")
    @RequiresPermissions("payment:payment:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(paymentInfoService.getById(id));
    }

    /**
     * 发起退款
     */
    @Operation(summary = "发起退款")
    @RequiresPermissions("payment:payment:refund")
    @Log(title = "支付管理", businessType = BusinessType.UPDATE)
    @PostMapping("/refund")
    public AjaxResult refund(@RequestParam String orderNo,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String reason,
                             @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && !idempotentService.consumeToken(idempotencyKey)) {
            return error("请勿重复提交");
        }
        paymentInfoService.refund(orderNo, amount, reason);
        return success();
    }
}
