package com.share.order.controller;

import com.share.common.redis.service.IdempotentService;
import com.github.pagehelper.PageHelper;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.dto.CreateOrderDTO;
import com.share.order.domain.vo.OrderSqlVo;
import com.share.order.service.IOrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端订单接口
 *
 * @author atguigu
 * @date 2024-02-22
 */
@Tag(name = "C端订单接口")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderInfoApiController extends BaseController {

    private final IOrderInfoService orderInfoService;
    private final IdempotentService idempotentService;

    @Operation(summary = "获取幂等Token")
    @RequiresLogin
    @GetMapping("/submit-token")
    public R<String> getSubmitToken() {
        return R.ok(idempotentService.generateToken());
    }

    @Operation(summary = "获取订单详细信息")
    @RequiresLogin
    @GetMapping("/getOrderInfo/{id}")
    public R<OrderInfo> getOrderInfo(@PathVariable Long id) {
        return R.ok(orderInfoService.selectOrderInfoById(id));
    }

    @Operation(summary = "获取用户订单分页列表")
    @RequiresLogin
    @GetMapping("/userOrderInfoList/{pageNum}/{pageSize}")
    public TableDataInfo list(@PathVariable @Min(1) Integer pageNum,
                              @PathVariable @Min(1) Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<OrderInfo> list =
                orderInfoService.selectOrderListByUserId(SecurityContextHolder.getUserId());
        return getDataTable(list);
    }

    @Operation(summary = "根据订单号获取订单信息")
    @RequiresLogin
    @GetMapping("/getByOrderNo/{orderNo}")
    public R<OrderInfo> getByOrderNo(@PathVariable @NotBlank String orderNo) {
        return R.ok(orderInfoService.getByOrderNo(orderNo));
    }

    @Operation(summary = "创建订单")
    @RequiresLogin
    @PostMapping("/createOrder")
    public R<String> createOrder(@Valid @RequestBody CreateOrderDTO dto,
                                  @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && !idempotentService.consumeToken(idempotencyKey)) {
            return R.fail("请勿重复提交");
        }
        String orderNo = orderInfoService.createOrder(dto);
        return R.ok(orderNo);
    }

    @Operation(summary = "取消订单")
    @RequiresLogin
    @PostMapping("/cancel/{orderNo}")
    public R<Void> cancel(@PathVariable @NotBlank String orderNo,
                          @RequestParam(defaultValue = "1") String closeType,
                          @RequestParam(defaultValue = "手动取消") String reason) {
        orderInfoService.cancelOrder(orderNo, closeType, reason);
        return R.ok();
    }

    @Operation(summary = "配送员发货")
    @RequiresLogin
    @PostMapping("/deliver/{orderNo}")
    public R<Void> deliver(@PathVariable @NotBlank String orderNo,
                           @RequestParam Long deliveryBy,
                           @RequestParam String deliveryName,
                           @RequestParam String deliveryPhone) {
        orderInfoService.deliverOrder(orderNo, deliveryBy, deliveryName, deliveryPhone);
        return R.ok();
    }

    @Operation(summary = "确认收货")
    @RequiresLogin
    @PostMapping("/confirm/{orderNo}")
    public R<Void> confirm(@PathVariable @NotBlank String orderNo) {
        orderInfoService.confirmReceive(orderNo);
        return R.ok();
    }

    @Operation(summary = "模拟支付成功（开发环境）")
    @RequiresLogin
    @PostMapping("/mockPay/{orderNo}")
    public R<Void> mockPay(@PathVariable @NotBlank String orderNo) {
        orderInfoService.processPaySuccess(orderNo, "mock_txn_" + System.currentTimeMillis());
        return R.ok();
    }

    @Operation(summary = "获取订单统计")
    @RequiresLogin
    @RequiresPermissions("order:info:count")
    @PostMapping("/getOrderCount")
    public R<Map<String, Object>> getOrderCount(@RequestBody OrderSqlVo orderSqlVo) {
        return R.ok(orderInfoService.getOrderCount(orderSqlVo.getSql()));
    }
}
