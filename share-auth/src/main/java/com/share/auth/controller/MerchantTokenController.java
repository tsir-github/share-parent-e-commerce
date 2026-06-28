package com.share.auth.controller;

import com.share.auth.service.MerchantLoginService;
import com.share.common.core.domain.R;
import com.share.common.core.utils.JwtUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.auth.AuthUtil;
import com.share.common.security.service.TokenService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家 Token 控制器
 *
 * <p>提供商家登录、登出、获取信息的端点。
 * 商家登录使用独立的 merchant_user 表，与管理员系统隔离。</p>
 *
 * @author share
 */
@RestController
@RequestMapping("/auth/merchant")
@RequiredArgsConstructor
public class MerchantTokenController {

    private final TokenService tokenService;
    private final MerchantLoginService merchantLoginService;

    /**
     * 商家登录
     *
     * POST /auth/merchant/login
     * 请求体：{"username":"shop001", "password":"xxx"}
     */
    @PostMapping("login")
    public R<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 校验账号密码
        LoginUser loginUser = merchantLoginService.login(username, password);

        // 生成 token（JWT + Redis）
        Map<String, Object> tokenMap = tokenService.createToken(loginUser);
        tokenMap.put("merchant", Map.of("id", loginUser.getMerchantId(), "name", loginUser.getUsername()));
        return R.ok(tokenMap);
    }

    /**
     * 商家登出
     *
     * POST /auth/merchant/logout
     */
    @PostMapping("logout")
    public R<?> logout(HttpServletRequest request) {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            AuthUtil.logoutByToken(token);
        }
        return R.ok();
    }

    /**
     * 获取当前商家信息
     *
     * GET /auth/merchant/getInfo
     */
    @GetMapping("getInfo")
    public R<?> getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return R.fail("未登录");
        }
        Map<String, Object> info = new HashMap<>();
        info.put("merchantId", loginUser.getMerchantId());
        info.put("username", loginUser.getUsername());
        info.put("name", loginUser.getUsername());
        return R.ok(info);
    }
}
