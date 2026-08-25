package com.share.merchant.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.merchant.domain.dto.ResetPasswordDTO;
import com.share.merchant.service.IMerchantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家账号管理 Controller（管理员端）
 *
 * @author share
 */
@Tag(name = "商家账号管理")
@RestController
@RequestMapping("/merchantInfo")
@RequiredArgsConstructor
public class MerchantAccountController extends BaseController {

    private final IMerchantUserService merchantUserService;

    @Operation(summary = "重置商家登录密码")
    @RequiresPermissions("merchant:merchant:edit")
    @Log(title = "商家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPassword")
    public AjaxResult resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        String newPassword = dto.getNewPassword() != null ? dto.getNewPassword() : "123456";
        String username = merchantUserService.resetPasswordByAdmin(dto.getMerchantId(), newPassword);
        return success("密码已重置")
                .put("username", username);
    }
}
