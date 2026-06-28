package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.OrderInfo;
import com.share.order.service.IMerchantOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商家端订单管理
 *
 * @author share
 */
@Tag(name = "商家端订单管理")
@RestController
@RequestMapping("/api/v1/merchant/order")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final IMerchantOrderService merchantOrderService;

    @Operation(summary = "商家订单列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<OrderInfo>> list() {
        return R.ok(merchantOrderService.selectMerchantOrderList(SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "商家订单详情")
    @RequiresLogin
    @GetMapping("/{id}")
    public R<OrderInfo> detail(@PathVariable Long id) {
        return R.ok(merchantOrderService.getMerchantOrder(id, SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "商家发货")
    @RequiresLogin
    @PostMapping("/deliver")
    public R<Void> deliver(@RequestBody Map<String, String> body) {
        merchantOrderService.deliverMerchantOrder(
                body.get("orderNo"), SecurityUtils.getMerchantId(),
                body.get("deliveryName"), body.get("deliveryPhone"));
        return R.ok();
    }

    @Operation(summary = "商家批量发货")
    @RequiresLogin
    @PostMapping("/batch-deliver")
    public R<Map<String, Object>> batchDeliver(@RequestBody List<Map<String, String>> items) {
        return R.ok(merchantOrderService.batchDeliver(items, SecurityUtils.getMerchantId()));
    }
}
