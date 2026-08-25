package com.share.goods.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.goods.domain.Banner;
import com.share.goods.service.IBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Banner 管理 Controller（管理员端）
 *
 * @author share
 */
@Tag(name = "Banner管理")
@RestController
@RequestMapping("/banner")
@RequiredArgsConstructor
public class BannerAdminController extends BaseController {

    private final IBannerService bannerService;

    @Operation(summary = "查询Banner列表")
    @RequiresPermissions("goods:banner:list")
    @GetMapping("/list")
    public TableDataInfo list(Banner banner) {
        startPage();
        List<Banner> list = bannerService.list();
        return getDataTable(list);
    }

    @Operation(summary = "获取Banner详情")
    @RequiresPermissions("goods:banner:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(bannerService.getById(id));
    }

    @Operation(summary = "新增Banner")
    @RequiresPermissions("goods:banner:add")
    @Log(title = "Banner管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Banner banner) {
        return toAjax(bannerService.save(banner));
    }

    @Operation(summary = "修改Banner")
    @RequiresPermissions("goods:banner:edit")
    @Log(title = "Banner管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Banner banner) {
        return toAjax(bannerService.updateById(banner));
    }

    @Operation(summary = "修改Banner状态")
    @RequiresPermissions("goods:banner:edit")
    @Log(title = "Banner管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult status(@RequestBody Banner banner) {
        return toAjax(bannerService.updateStatus(banner.getId(), banner.getStatus()));
    }

    @Operation(summary = "删除Banner")
    @RequiresPermissions("goods:banner:remove")
    @Log(title = "Banner管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(bannerService.removeBatchByIds(Arrays.asList(ids)));
    }
}
