package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.service.IMerchantInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 商家信息 Controller（管理员端）
 *
 * @author share
 */
@Tag(name = "商家信息管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/merchant")
public class MerchantInfoController extends BaseController {

    private final IMerchantInfoService merchantInfoService;

    @Operation(summary = "查询商家列表")
    @RequiresPermissions("merchant:merchant:list")
    @GetMapping("/list")
    public TableDataInfo list(MerchantInfo merchantInfo) {
        startPage();
        List<MerchantInfo> list = merchantInfoService.list();
        return getDataTable(list);
    }

    @Operation(summary = "获取商家详细信息")
    @RequiresPermissions("merchant:merchant:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(merchantInfoService.getById(id));
    }

    @Operation(summary = "新增商家")
    @RequiresPermissions("merchant:merchant:add")
    @Log(title = "商家信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MerchantInfo merchantInfo) {
        return toAjax(merchantInfoService.save(merchantInfo));
    }

    @Operation(summary = "修改商家")
    @RequiresPermissions("merchant:merchant:edit")
    @Log(title = "商家信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MerchantInfo merchantInfo) {
        return toAjax(merchantInfoService.updateById(merchantInfo));
    }

    @Operation(summary = "删除商家")
    @RequiresPermissions("merchant:merchant:remove")
    @Log(title = "商家信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(merchantInfoService.removeBatchByIds(Arrays.asList(ids)));
    }

    @Operation(summary = "审核商家")
    @RequiresPermissions("merchant:merchant:audit")
    @Log(title = "商家信息", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody MerchantInfo merchantInfo) {
        merchantInfoService.audit(merchantInfo);
        return success();
    }

    /**
     * 商家仪表盘统计数据
     */
    @Operation(summary = "商家仪表盘统计")
    @RequiresPermissions("merchant:merchant:list")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(merchantInfoService.getDashboardStats());
    }
}
