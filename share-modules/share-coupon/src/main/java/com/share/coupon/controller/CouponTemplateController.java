package com.share.coupon.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.coupon.domain.CouponTemplate;
import com.share.coupon.service.ICouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券模板Controller（B端管理）
 *
 * @author share
 */
@Tag(name = "优惠券模板管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/couponTemplate")
public class CouponTemplateController extends BaseController {

    private final ICouponTemplateService couponTemplateService;

    @Operation(summary = "查询优惠券模板列表")
    @RequiresPermissions("coupon:template:list")
    @GetMapping("/list")
    public TableDataInfo list(CouponTemplate template) {
        startPage();
        List<CouponTemplate> list = couponTemplateService.list();
        return getDataTable(list);
    }

    @Operation(summary = "获取优惠券模板详情")
    @RequiresPermissions("coupon:template:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(couponTemplateService.getById(id));
    }

    @Operation(summary = "新增优惠券模板")
    @RequiresPermissions("coupon:template:add")
    @Log(title = "优惠券模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CouponTemplate template) {
        return toAjax(couponTemplateService.save(template));
    }

    @Operation(summary = "修改优惠券模板")
    @RequiresPermissions("coupon:template:edit")
    @Log(title = "优惠券模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CouponTemplate template) {
        return toAjax(couponTemplateService.updateById(template));
    }

    @Operation(summary = "删除优惠券模板")
    @RequiresPermissions("coupon:template:remove")
    @Log(title = "优惠券模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(couponTemplateService.removeById(id));
    }
}
