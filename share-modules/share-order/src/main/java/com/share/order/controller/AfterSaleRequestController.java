package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.core.web.controller.BaseController;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.AfterSaleRequest;
import com.share.order.service.IAfterSaleRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 售后申请Controller（C端用户）
 *
 * <p>管理端接口已迁移至 {@link AfterSaleRequestAdminController}。</p>
 *
 * 路径说明（Gateway StripPrefix=1）：
 *   外部请求 /order/api/v1/order/after-sale/apply → 转发到 /api/v1/order/after-sale/apply
 */
@Tag(name = "售后申请（用户端）")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order/after-sale")
public class AfterSaleRequestController extends BaseController {

    private final IAfterSaleRequestService afterSaleRequestService;

    @Operation(summary = "用户发起售后申请")
    @RequiresLogin
    @PostMapping("/apply")
    public R<Void> apply(@RequestParam String orderNo,
                         @RequestParam(required = false) BigDecimal refundAmount,
                         @RequestParam(required = false) String refundReason) {
        afterSaleRequestService.apply(orderNo, refundAmount, refundReason);
        return R.ok();
    }

    @Operation(summary = "查询我的售后申请列表")
    @RequiresLogin
    @GetMapping("/my")
    public TableDataInfo myList() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        return getDataTable(afterSaleRequestService.selectMyList(userId));
    }

    @Operation(summary = "查询售后申请详情")
    @RequiresLogin
    @GetMapping("/detail")
    public R<AfterSaleRequest> detail(@RequestParam Long id) {
        AfterSaleRequest request = afterSaleRequestService.getById(id);
        if (request == null || !SecurityUtils.getUserId().equals(request.getUserId())) {
            return R.fail("售后申请不存在");
        }
        return R.ok(request);
    }
}
