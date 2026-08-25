package com.share.order.controller;

import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.order.domain.dto.SeckillOrderRequest;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.dto.CreateOrderDTO;
import com.share.order.domain.vo.OrderSqlVo;
import com.share.order.service.IAfterSaleRequestService;
import com.share.order.service.IOrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private final IAfterSaleRequestService afterSaleRequestService;

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

    @Operation(summary = "商家控制台统计（内部）")
    @InnerAuth
    @GetMapping("/getMerchantDashboard/{merchantId}")
    public R<Map<String, Object>> getMerchantDashboard(@PathVariable Long merchantId) {
        Map<String, Object> data = new HashMap<>();
        data.put("todayOrders", orderInfoService.countTodayOrders(merchantId));
        data.put("todaySales", orderInfoService.sumTodaySales(merchantId));
        data.put("pendingDelivery", orderInfoService.countPendingDelivery(merchantId));
        data.put("pendingAfterSale", afterSaleRequestService.countPendingByMerchant(merchantId));
        return R.ok(data);
    }

    @Operation(summary = "创建秒杀订单（内部）")
    @InnerAuth
    @PostMapping("/createSeckillOrder")
    public R<String> createSeckillOrder(@RequestBody SeckillOrderRequest request) {
        // 设置用户上下文（Feign 调用时无 JWT，手动设置）
        SecurityContextHolder.setUserId(String.valueOf(request.getUserId()));
        try {
            // 构造 CreateOrderDTO
            CreateOrderDTO dto = new CreateOrderDTO();
            dto.setOrderNo(request.getOrderNo());     // 复用预生成订单号，MQ 重投时 DB 唯一约束防重复
            dto.setSeckillActivityId(request.getSeckillActivityId());
            dto.setSeckillPrice(request.getSeckillPrice());
            dto.setReceiverName(request.getReceiverName());
            dto.setReceiverPhone(request.getReceiverPhone());
            dto.setReceiverAddress(request.getReceiverAddress());
            dto.setRemark(request.getRemark());

            List<CreateOrderDTO.OrderItemDTO> items = request.getItems().stream()
                    .map(i -> {
                        CreateOrderDTO.OrderItemDTO item = new CreateOrderDTO.OrderItemDTO();
                        item.setSkuId(i.getSkuId());
                        item.setQuantity(i.getQuantity());
                        item.setProductName(i.getProductName());
                        item.setSkuSpecs(i.getSkuSpecs());
                        return item;
                    }).toList();
            dto.setItems(items);

            String orderNo = orderInfoService.createOrder(dto);
            return R.ok(orderNo);
        } finally {
            SecurityContextHolder.remove();
        }
    }
}
