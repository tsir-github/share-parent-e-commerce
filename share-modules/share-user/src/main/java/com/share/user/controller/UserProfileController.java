package com.share.user.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.user.domain.UserInfo;
import com.share.user.service.IUserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * C端用户个人信息管理
 *
 * @author share
 */
@Tag(name = "C端用户信息")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserInfoService userInfoService;

    @Operation(summary = "获取当前用户信息")
    @RequiresLogin
    @GetMapping("/profile")
    public R<UserInfo> getProfile() {
        return R.ok(userInfoService.getCurrentUser());
    }

    @Operation(summary = "修改昵称")
    @RequiresLogin
    @PutMapping("/profile/nickname")
    public R<Void> updateNickname(@RequestBody Map<String, String> body) {
        String nickname = body.get("nickname");
        if (nickname == null || nickname.trim().isEmpty()) {
            return R.fail("昵称不能为空");
        }
        if (nickname.length() > 30) {
            return R.fail("昵称长度不能超过30个字符");
        }
        userInfoService.updateNickname(nickname.trim());
        return R.ok();
    }

    @Operation(summary = "修改性别")
    @RequiresLogin
    @PutMapping("/profile/gender")
    public R<Void> updateGender(@RequestBody Map<String, String> body) {
        String gender = body.get("gender");
        if (!"0".equals(gender) && !"1".equals(gender)) {
            return R.fail("无效的性别值（0=男 1=女）");
        }
        userInfoService.updateGender(gender);
        return R.ok();
    }

    @Operation(summary = "修改手机号")
    @RequiresLogin
    @PutMapping("/profile/phone")
    public R<Void> updatePhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            return R.fail("手机号格式不正确");
        }
        userInfoService.updatePhone(phone);
        return R.ok();
    }
}
