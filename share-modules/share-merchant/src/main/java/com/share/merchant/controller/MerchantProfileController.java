package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.merchant.domain.MerchantInfo;
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
    public R<Void> updatePassword(@RequestParam String newPassword) {
        merchantUserService.updatePassword(SecurityUtils.getUserId(), newPassword);
        return R.ok();
    }

    @Operation(summary = "上传店铺Logo")
    @RequiresLogin
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            return R.fail("只允许上传图片文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return R.fail("图片大小不能超过10MB");
        }
        String url = merchantInfoService.updateLogo(SecurityUtils.getMerchantId(), file);
        return R.ok(Map.of("logoUrl", url));
    }
}
