package com.share.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.share.auth.form.LoginBody;
import com.share.auth.form.RegisterBody;
import com.share.auth.service.SysLoginService;
import com.share.common.core.domain.R;
import com.share.common.core.utils.JwtUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.auth.AuthUtil;
import com.share.common.security.service.TokenService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.model.LoginUser;

/**
 * Token 控制器 — 登录流程的"入口"
 *
 * 登录全流程中的后端第一站：
 * 前端 POST /auth/login 请求会经过 网关 → 被路由到认证中心（share-auth）→ 最终到达这里。
 *
 * 这个控制器只做三件事：
 *   1. login()    → 校验账号密码 + 生成 token（JWT + Redis）
 *   2. logout()   → 删除 Redis 中的 token（注销登录）
 *   3. refresh()  → 刷新 token 有效期（续期）
 *
 * @author share
 */
@RestController
public class TokenController
{
    @Autowired
    private TokenService tokenService;
    // TokenService 负责生成 JWT 和操作 Redis
    // 关键方法：createToken() → 生成 UUID → 存 Redis → 创建 JWT

    @Autowired
    private SysLoginService sysLoginService;
    // SysLoginService 负责校验账号密码
    // 关键方法：login() → 查数据库 → 验证密码 → 返回 LoginUser

    /**
     * ★ 登录接口
     *
     * 前端调用：POST /auth/login
     * 请求体：{"username":"admin", "password":"admin123"}
     *
     * 执行流程：
     *   第 1 步：sysLoginService.login() 校验账号密码
     *     - 参数校验（空值、长度）
     *     - IP 黑名单校验
     *     - Feign 调用 system 服务查数据库
     *     - 密码校验
     *     - 返回 LoginUser 对象（含用户ID、用户名、角色、权限等）
     *
     *   第 2 步：tokenService.createToken() 生成令牌
     *     - 生成随机 UUID
     *     - 把 LoginUser 存入 Redis（key = "login_tokens:UUID"）
     *     - 用 UUID 生成 JWT
     *     - 返回 { access_token: "eyJ...", expires_in: 720 }
     *
     *   第 3 步：返回给前端
     *     前端拿到 access_token 后存到 Cookie 里
     *     后续所有请求都带着这个 JWT
     *
     * @param form 前端提交的登录表单（username + password）
     * @return R.ok({ access_token: "JWT字符串", expires_in: 720 })
     */
    @PostMapping("login")
    public R<?> login(@RequestBody LoginBody form)
    {
        // ★ 第 1 步：校验用户名密码，从数据库查询用户信息
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());

        // ★ 第 2 步：生成 token（JWT）并存 Redis
        // createToken 返回的 Map 包含：
        //   access_token → 发给前端的 JWT
        //   expires_in   → token 有效期（分钟）
        return R.ok(tokenService.createToken(userInfo));
    }

    /**
     * 退出登录
     *
     * 前端调用：DELETE /auth/logout
     * 请求头：Authorization: Bearer eyJ...
     *
     * 做的事情：
     *   1. 从请求头取出 JWT
     *   2. 解析 JWT 拿到用户名（用于记日志）
     *   3. 删除 Redis 中对应的 key（让 token 失效）
     *   4. 记录退出日志
     *
     * 注意：前端也需要清除本地的 Cookie（Admin-Token）
     *
     * @param request HTTP 请求，里面包含 Authorization 请求头
     * @return R.ok()
     */
    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request)
    {
        // 从请求头 Authorization: Bearer xxx 中取出 JWT
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            // 解析 JWT 拿到用户名（用于记日志）
            String username = JwtUtils.getUserName(token);

            // ★ 关键操作：删除 Redis 中的 LoginUser 缓存
            // 删除后，redisService.hasKey("login_tokens:UUID") 返回 false
            // 后续再用这个 JWT 访问其他接口时，网关会返回 401 未授权
            AuthUtil.logoutByToken(token);

            // 记录退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    /**
     * 刷新 token 有效期
     *
     * 前端调用：POST /auth/refresh
     *
     * 当 token 剩余有效期不足 120 分钟时，
     * 延长 Redis 中 LoginUser 的过期时间（重新计时 720 分钟）。
     * 这样用户持续操作时不会突然过期。
     *
     * @param request HTTP 请求
     * @return R.ok()
     */
    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request)
    {
        // 从请求头取 JWT → 解析出 user_key → 查 Redis 拿到 LoginUser
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新 Redis 过期时间，重置为 720 分钟
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    /**
     * 用户注册
     *
     * @param registerBody 注册信息
     * @return R.ok()
     */
    @PostMapping("register")
    public R<?> register(@RequestBody RegisterBody registerBody)
    {
        // 注册新用户
        sysLoginService.register(registerBody.getUsername(), registerBody.getPassword());
        return R.ok();
    }
}
