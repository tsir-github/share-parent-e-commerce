package com.share.user.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.user.service.IUserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * C端用户头像上传
 *
 * @author share
 */
@Tag(name = "用户头像")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserAvatarController {

    private final IUserInfoService userInfoService;

    @Operation(summary = "上传更换头像")
    @RequiresLogin
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return R.fail("只允许上传图片文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return R.fail("图片大小不能超过10MB");
        }
        String url = userInfoService.updateAvatar(file);
        return R.ok(Map.of("avatarUrl", url));
    }
}
