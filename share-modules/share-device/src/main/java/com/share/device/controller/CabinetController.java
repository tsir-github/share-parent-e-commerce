package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.Cabinet;
import com.share.device.service.ICabinetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "充电宝柜机接口管理")
@RestController
@RequestMapping("/cabinet")
public class CabinetController extends BaseController {

    @Autowired
    private ICabinetService cabinetService;

    @Operation(summary = "搜索未使用的柜机")
    @GetMapping(value = "/searchNoUseList/{keyword}")
    private AjaxResult searchNoUseList(@PathVariable String keyword){
        return success(cabinetService.searchNoUseList(keyword));
    }

    @Operation(summary = "修改充电宝柜机")
    @PutMapping
    public AjaxResult edit(@RequestBody Cabinet cabinet)
    {
        boolean updateById = cabinetService.updateById(cabinet);
        AjaxResult ajaxResult = toAjax(updateById);
        return ajaxResult;
    }

    @Operation(summary = "删除充电宝柜机")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(cabinetService.removeBatchByIds(Arrays.asList(ids)));
    }

    @Operation(summary = "新增充电宝柜机")
    @PostMapping
    public AjaxResult add(@RequestBody  Cabinet cabinet){
        boolean flag = cabinetService.save(cabinet);
        AjaxResult ajaxResult = toAjax(flag);
        return ajaxResult;
    }

    @Operation(summary = "获取充电宝柜机详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id){
        Cabinet byId = cabinetService.getById(id);
        AjaxResult ajaxResult = success(byId);//等价于super。success(byId);
        return ajaxResult;
        //return success(byId);
    }

    //分页查询
    @Operation(summary = "查询充电宝柜机列表")
    @GetMapping("/list")
    public TableDataInfo list(Cabinet cabinet){
        //分页参数
        startPage();
        //调用service方法
        List<Cabinet> list= cabinetService.selectCabinetList(cabinet);
        TableDataInfo dataTable = getDataTable(list);
        return dataTable;
    }

}
