package com.share.merchant.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.domain.dto.MerchantStatusDTO;
import com.share.merchant.domain.vo.MerchantInfoVO;
import com.share.merchant.service.IMerchantInfoService;
import com.share.merchant.service.IMerchantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
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
@RequestMapping("/merchantInfo")
public class MerchantInfoController extends BaseController {

    private final IMerchantInfoService merchantInfoService;
    private final IMerchantUserService merchantUserService;

    @Operation(summary = "查询商家列表")
    @RequiresPermissions("merchant:merchant:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) String status) {
        startPage();
        List<MerchantInfoVO> list = merchantInfoService.selectListWithAccount(name, status);
        return getDataTable(list);
    }

    @Operation(summary = "获取商家详细信息")
    @RequiresPermissions("merchant:merchant:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        MerchantInfo info = merchantInfoService.getById(id);
        if (info == null) {
            return error("商家不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("merchant", info);
        // 查询关联登录账号
        MerchantUser account = merchantUserService.getByMerchantId(id);
        if (account != null) {
            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put("username", account.getUsername());
            accountInfo.put("status", account.getStatus());
            accountInfo.put("loginIp", account.getLoginIp());
            accountInfo.put("loginDate", account.getLoginDate());
            accountInfo.put("createTime", account.getCreateTime());
            result.put("account", accountInfo);
        }
        return success(result);
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
        merchantInfoService.adminUpdate(merchantInfo);
        return success();
    }

    @Operation(summary = "删除商家")
    @RequiresPermissions("merchant:merchant:remove")
    @Log(title = "商家信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            // 同步逻辑删除关联登录账号
            merchantUserService.logicDeleteByMerchantId(id);
        }
        return toAjax(merchantInfoService.removeBatchByIds(Arrays.asList(ids)));
    }

    @Operation(summary = "修改商家状态")
    @RequiresPermissions("merchant:merchant:edit")
    @Log(title = "商家信息", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult status(@Valid @RequestBody MerchantStatusDTO dto) {
        merchantInfoService.updateStatus(dto.getId(), dto.getStatus());
        merchantUserService.syncStatusByMerchantId(dto.getId(), dto.getStatus());
        return success();
    }

    @Operation(summary = "审核商家")
    @RequiresPermissions("merchant:merchant:audit")
    @Log(title = "商家信息", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody MerchantInfo merchantInfo) {
        MerchantUser user = merchantInfoService.audit(merchantInfo);
        AjaxResult result = success();
        if (user != null) {
            result.put("username", user.getUsername());
            result.put("msg", "审核通过，商家登录账号：" + user.getUsername());
        }
        return result;
    }

    /**
     * 商家仪表盘统计数据
     */
    @Operation(summary = "商家仪表盘统计")
    @RequiresPermissions("merchant:merchant:list")
    @GetMapping("/dashboard")
    public AjaxResult dashboard() {
        return success(merchantInfoService.getDashboardStats());
    }
}
