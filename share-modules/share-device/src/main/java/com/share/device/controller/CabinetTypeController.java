package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.device.domain.CabinetType;
import com.share.device.service.ICabinetTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Tag(name = "柜机类型接口管理")
@RestController
@RequestMapping("/cabinetType")
public class CabinetTypeController extends BaseController
{
    @Autowired
    private ICabinetTypeService cabinetTypeService;


    @Operation(summary = "查询全部柜机类型列表")
    @GetMapping("/getCabinetTypeList")
    public AjaxResult getCabinetTypeList(){
        return success(cabinetTypeService.list());
    }

    @Operation(summary = "删除柜机类型")
    @RequiresPermissions("device:cabinetType:remove")
    @Log(title = "柜机类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable Long[] ids){//拿到前端传过来的json数据，是个数组
        boolean is_Success = cabinetTypeService.removeBatchByIds(Arrays.asList(ids));//数组变集合
        AjaxResult ajaxResult = toAjax(is_Success);
        return ajaxResult;

    }

    //修改时要传id,添加时不用
    @Operation(summary = "修改柜机类型")
    @RequiresPermissions("device:cabinetType:edit")
    @Log(title = "柜机类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult update(@RequestBody @Validated CabinetType cabinetType){
        Date now = new Date();
        //cabinetType.setCreateTime(now);
        cabinetType.setUpdateTime(now);
        boolean is_Success = cabinetTypeService.updateById(cabinetType);
        return toAjax(is_Success);
    }

    @Operation(summary = "新增柜机类型")
    @RequiresPermissions("device:cabinetType:add")
    @Log(title = "柜机类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody @Validated CabinetType cabinetType){
        // 手动设置时间
        Date now = new Date();
        cabinetType.setCreateTime(now);
        //cabinetType.setUpdateTime(now);
        boolean is_Success = cabinetTypeService.save(cabinetType);
        return toAjax(is_Success);

    }

    @Operation(summary = "根据id查询详情")
    @RequiresPermissions("device:cabinetType:query")
    @GetMapping("{id}")
    public AjaxResult getCabinetType(@PathVariable("id") Long id){
        CabinetType cabinetType = cabinetTypeService.getById(id);
        return success(cabinetType);
    }
    /**
     * 查询柜机类型列表
     */
    @Operation(summary = "分页查询柜机类型列表")
    @Log(title = "柜机类型查询", businessType = BusinessType.OTHER)
    @RequiresPermissions("device:cabinetType:list")
    @GetMapping("/list")
    public TableDataInfo list(CabinetType cabinetType)
    {
        //分页查询返回TableDataInfo，普通就返回AjaxResult
        //分装分页参数数据
        startPage();
        //调用service查询数据库
        List<CabinetType> list = cabinetTypeService.selectCabinetTypeList(cabinetType);

        return getDataTable(list);
    }

}