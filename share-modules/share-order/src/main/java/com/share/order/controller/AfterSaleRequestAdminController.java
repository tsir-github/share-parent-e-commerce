package com.share.order.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.order.domain.AfterSaleRequest;
import com.share.order.service.IAfterSaleRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 售后申请管理（管理员端）
 *
 * <p>与 {@link AfterSaleRequestController} 分离，管理端接口不走 {@code /api/v1} 前缀。</p>
 *
 * @author share
 */
@Tag(name = "售后申请管理（管理员端）")
@RestController
@RequestMapping("/after-sale")
@RequiredArgsConstructor
public class AfterSaleRequestAdminController extends BaseController {

    private final IAfterSaleRequestService afterSaleRequestService;

    /**
     * 查询售后申请列表
     */
    @Operation(summary = "查询售后申请列表（管理端）")
    @RequiresPermissions("order:after-sale:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String auditStatus) {
        startPage();
        return getDataTable(afterSaleRequestService.selectList(auditStatus));
    }

    /**
     * 商家审核售后申请
     */
    @Operation(summary = "商家审核售后申请")
    @RequiresPermissions("order:after-sale:audit")
    @Log(title = "售后申请", businessType = BusinessType.UPDATE)
    @PostMapping("/audit")
    public AjaxResult audit(@RequestParam Long id,
                            @RequestParam String auditStatus,
                            @RequestParam(required = false) String auditRemark) {
        afterSaleRequestService.audit(id, auditStatus, auditRemark);
        return success();
    }

    /**
     * 客服处理售后申请
     */
    @Operation(summary = "客服处理售后申请")
    @RequiresPermissions("order:after-sale:admin-audit")
    @Log(title = "售后申请", businessType = BusinessType.UPDATE)
    @PostMapping("/admin-audit")
    public AjaxResult adminAudit(@RequestParam Long id,
                                 @RequestParam String auditStatus,
                                 @RequestParam(required = false) String auditRemark) {
        afterSaleRequestService.adminAudit(id, auditStatus, auditRemark);
        return success();
    }
}
