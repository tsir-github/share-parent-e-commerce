package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;

import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.common.security.utils.SecurityUtils;
import com.share.device.domain.PowerBank;
import com.share.device.service.IPowerBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Tag(name = "充电宝接口管理")
@RestController
@RequestMapping("/powerBank")
public class PowerBankController extends BaseController {

    @Autowired
    private IPowerBankService powerBankService;

    //分页查询
    @Operation(summary = "查询充电宝列表")
    @GetMapping("/list")
    public TableDataInfo list(PowerBank powerBank){
        //分页参数
        startPage();
        //调用service方法
        List<PowerBank> list= powerBankService.selectPowerBankList(powerBank);
        return getDataTable(list);
    }

    @Operation(summary = "获取充电宝详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id){
        PowerBank byId = powerBankService.getById(id);
        return success(byId);
    }

    //添加
    @Operation(summary = "新增充电宝")
    @RequiresPermissions("device:powerBank:add")
    @PostMapping
    public AjaxResult add(@RequestBody PowerBank powerBank){
        //设置相关数据的值
        powerBank.setCreateBy(SecurityUtils.getUsername());
        powerBank.setCreateTime(new Date());

        return toAjax(powerBankService.savePowerBank(powerBank));
    }
    //修改
    @Operation(summary = "修改充电宝")
    @PutMapping
    public AjaxResult update(@RequestBody PowerBank powerBank){
        powerBank.setUpdateBy(SecurityUtils.getUsername());
        powerBank.setUpdateTime(new Date());
       //powerBankService.updatePowerBank(powerBank);
        return toAjax(powerBankService.updatePowerBank(powerBank));
    }
    //删除
    @Operation(summary = "删除充电宝")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        boolean b = powerBankService.removeBatchByIds(Arrays.asList(ids));
        return toAjax(b);

    }

}
