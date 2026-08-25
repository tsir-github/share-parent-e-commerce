package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.service.IMerchantInfoService;
import com.share.merchant.service.IMerchantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 商家店铺资料
 *
 * <p>商家查看/编辑自己的店铺信息、修改密码。</p>
 *
 * @author share
 */
@Tag(name = "商家店铺资料")
@RestController
@RequestMapping("/api/v1/merchant/profile")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final IMerchantInfoService merchantInfoService;
    private final IMerchantUserService merchantUserService;

    @Operation(summary = "获取店铺信息")
    @RequiresLogin
    @GetMapping
    public R<MerchantInfo> getProfile() {
        return R.ok(merchantInfoService.getMerchantProfile(SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "更新店铺信息")
    @RequiresLogin
    @PutMapping
    public R<Void> updateProfile(@RequestBody MerchantInfo update) {
        merchantInfoService.updateMerchantProfile(SecurityUtils.getMerchantId(), update);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @RequiresLogin
    @PutMapping("/password")
    public R<Void> updatePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return R.fail("旧密码和新密码不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        MerchantUser user = merchantUserService.getById(userId);
        if (user == null || !SecurityUtils.matchesPassword(oldPassword, user.getPassword())) {
            return R.fail("旧密码错误");
        }
        merchantUserService.updatePassword(userId, newPassword);
        return R.ok();
    }

    @Operation(summary = "更新商家个人资料（手机号、邮箱）")
    @RequiresLogin
    @PutMapping("/user")
    public R<Void> updateUserInfo(@RequestBody MerchantUser update) {
        merchantUserService.updateUserInfo(
            SecurityUtils.getUserId(),
            update.getPhone(),
            update.getEmail()
        );
        return R.ok();
    }

    @Operation(summary = "获取商家个人资料（手机号、邮箱）")
    @RequiresLogin
    @GetMapping("/user")
    public R<java.util.Map<String, String>> getUserInfo() {
        MerchantUser user = merchantUserService.getById(SecurityUtils.getUserId());
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (user != null) {
            map.put("phone", user.getPhone() != null ? user.getPhone() : "");
            map.put("email", user.getEmail() != null ? user.getEmail() : "");
        }
        return R.ok(map);
    }

    @Operation(summary = "上传店铺Logo")
    @RequiresLogin
    @PostMapping("/logo")
    public R<Void> updateLogo(@RequestBody Map<String, String> body) {
        String logoUrl = body.get("logoUrl");
        if (logoUrl == null || logoUrl.isEmpty()) {
            return R.fail("Logo URL不能为空");
        }
        Long merchantId = SecurityUtils.getMerchantId();
        merchantInfoService.updateLogoUrl(merchantId, logoUrl);
        return R.ok();
    }
}
