package com.share.goods.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.goods.domain.SeckillActivity;
import com.share.goods.service.ISeckillActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀活动Controller（管理员端）
 *
 * @author share
 */
@Tag(name = "秒杀活动管理")
@RestController
@RequestMapping("/seckillActivity")
@RequiredArgsConstructor
public class SeckillActivityController extends BaseController {

    private final ISeckillActivityService seckillActivityService;

    @Operation(summary = "查询秒杀活动列表")
    @RequiresPermissions("goods:seckill:list")
    @GetMapping("/list")
    public TableDataInfo list(SeckillActivity activity) {
        startPage();
        List<SeckillActivity> list = seckillActivityService.selectSeckillActivityList(activity);
        return getDataTable(list);
    }

    @Operation(summary = "获取秒杀活动详细信息")
    @RequiresPermissions("goods:seckill:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(seckillActivityService.selectSeckillActivityById(id));
    }

    @Operation(summary = "新增秒杀活动")
    @RequiresPermissions("goods:seckill:add")
    @Log(title = "秒杀活动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SeckillActivity activity) {
        return toAjax(seckillActivityService.insertSeckillActivity(activity));
    }

    @Operation(summary = "修改秒杀活动")
    @RequiresPermissions("goods:seckill:edit")
    @Log(title = "秒杀活动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SeckillActivity activity) {
        return toAjax(seckillActivityService.updateSeckillActivity(activity));
    }

    @Operation(summary = "修改秒杀活动状态（启用/禁用）")
    @RequiresPermissions("goods:seckill:edit")
    @Log(title = "秒杀活动", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult status(@RequestBody SeckillActivity activity) {
        return toAjax(seckillActivityService.updateStatus(activity.getId(), activity.getStatus()));
    }

    @Operation(summary = "删除秒杀活动")
    @RequiresPermissions("goods:seckill:remove")
    @Log(title = "秒杀活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(seckillActivityService.deleteSeckillActivityByIds(ids));
    }

    @Operation(summary = "获取秒杀活动统计")
    @RequiresPermissions("goods:seckill:query")
    @GetMapping("/stats/{id}")
    public AjaxResult stats(@PathVariable Long id) {
        SeckillActivity activity = seckillActivityService.selectSeckillActivityById(id);
        if (activity == null) {
            return error("秒杀活动不存在");
        }
        // ponytail: 占位统计，full data depends on future C端秒杀订单表
        return success(new SeckillStatsVO(activity));
    }

    /**
     * 秒杀活动统计占位VO
     */
    record SeckillStatsVO(Long id, String name, Integer totalStock, Integer salesAmount) {
        SeckillStatsVO(SeckillActivity a) {
            this(a.getId(), a.getName(), a.getStock(), 0);
        }
    }
}
