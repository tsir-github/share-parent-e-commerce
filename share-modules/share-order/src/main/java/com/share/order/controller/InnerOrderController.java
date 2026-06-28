package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.vo.OrderSqlVo;
import com.share.order.service.IOrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单内部接口（Feign 调用）
 *
 * @author share
 */
@Tag(name = "订单内部接口")
@RestController
@RequestMapping("/inner/order")
@RequiredArgsConstructor
public class InnerOrderController {

    private final IOrderInfoService orderInfoService;

    @Operation(summary = "根据订单号获取订单信息（内部）")
    @InnerAuth
    @GetMapping("/getByOrderNo/{orderNo}")
    public R<OrderInfo> getByOrderNo(@PathVariable String orderNo) {
        return R.ok(orderInfoService.getByOrderNo(orderNo));
    }

    @Operation(summary = "获取用户未完成订单（内部）")
    @InnerAuth
    @GetMapping("/getNoFinishOrder/{userId}")
    public R<OrderInfo> getNoFinishOrder(@PathVariable Long userId) {
        return R.ok(orderInfoService.selectNoFinishOrder(userId));
    }

    @Operation(summary = "获取订单统计（内部）")
    @InnerAuth
    @PostMapping("/getOrderCount")
    public R<Map<String, Object>> getOrderCount(@RequestBody OrderSqlVo orderSqlVo) {
        return R.ok(orderInfoService.getOrderCount(orderSqlVo.getSql()));
    }

    @Operation(summary = "支付成功回调（内部）")
    @InnerAuth
    @PostMapping("/paySuccess/{orderNo}/{transactionId}")
    public R<Void> paySuccess(@PathVariable String orderNo, @PathVariable String transactionId) {
        orderInfoService.processPaySuccess(orderNo, transactionId);
        return R.ok();
    }
}
