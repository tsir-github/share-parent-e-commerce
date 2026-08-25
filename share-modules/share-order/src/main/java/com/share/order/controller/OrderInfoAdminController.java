package com.share.order.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.order.domain.OrderInfo;
import com.share.order.service.IOrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单管理（管理员端）
 *
 * <p>与 {@link OrderInfoApiController} 共用 {@code /orderInfo} 路径前缀，
 * 方法级路径不冲突（管理端使用 {@code /list}、{@code /{id}} 等标准 CRUD 路径）。</p>
 *
 * @author share
 */
@Tag(name = "订单管理（管理员端）")
@RestController
@RequestMapping("/orderInfo")
@RequiredArgsConstructor
public class OrderInfoAdminController extends BaseController {

    private final IOrderInfoService orderInfoService;

    /**
     * 查询订单列表
     */
    @Operation(summary = "查询订单列表")
    @RequiresPermissions("order:orderInfo:list")
    @GetMapping("/list")
    public TableDataInfo list(OrderInfo orderInfo) {
        startPage();
        List<OrderInfo> list = orderInfoService.selectOrderListForAdmin(orderInfo);
        return getDataTable(list);
    }

    /**
     * 获取订单详细信息
     */
    @Operation(summary = "获取订单详细信息")
    @RequiresPermissions("order:orderInfo:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(orderInfoService.selectOrderInfoById(id));
    }

    /**
     * 订单仪表盘统计数据
     */
    @Operation(summary = "订单仪表盘统计")
    @RequiresPermissions("order:orderInfo:list")
    @GetMapping("/dashboard")
    public AjaxResult dashboard() {
        return success(orderInfoService.getDashboardStats());
    }
}
