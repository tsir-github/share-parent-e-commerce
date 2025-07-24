package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.CabinetType;
import com.share.device.service.ICabinetTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable Long[] ids){
        boolean is_Success = cabinetTypeService.removeBatchByIds(Arrays.asList(ids));//数组变集合
        AjaxResult ajaxResult = toAjax(is_Success);
        return ajaxResult;

    }

    //修改时要传id,添加时不用
    @Operation(summary = "修改柜机类型")
    @PutMapping
    public AjaxResult update(@RequestBody @Validated CabinetType cabinetType){
        boolean is_Success = cabinetTypeService.updateById(cabinetType);
        AjaxResult ajaxResult = toAjax(is_Success);
        return ajaxResult;
    }

    @Operation(summary = "新增柜机类型")
    @PostMapping
    public AjaxResult add(@RequestBody @Validated CabinetType cabinetType){
        boolean is_Success = cabinetTypeService.save(cabinetType);
        AjaxResult ajaxResult = toAjax(is_Success);
        return ajaxResult;

    }

    @Operation(summary = "根据id查询详情")
    @GetMapping("{id}")
    public AjaxResult getCabinetType(@PathVariable("id") Long id){
        CabinetType cabinetType = cabinetTypeService.getById(id);
        AjaxResult ajaxResult=success(cabinetType);
        return ajaxResult;
    }
    /**
     * 查询柜机类型列表
     */
    @Operation(summary = "查询柜机类型列表")
    @GetMapping("/list")
    public TableDataInfo list(CabinetType cabinetType)
    {
        //分装分页参数数据
        startPage();
        //调用service查询数据库
        List<CabinetType> list = cabinetTypeService.selectCabinetTypeList(cabinetType);

        return getDataTable(list);
    }

}