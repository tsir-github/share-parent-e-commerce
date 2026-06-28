package com.share.common.security.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.utils.JwtUtils;
import com.share.common.core.utils.ServletUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.core.utils.ip.IpUtils;
import com.share.common.core.utils.uuid.IdUtils;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.model.LoginUser;

/**
 * Token 验证处理
 *
 * 登录流程中最核心的服务类。
 * 负责三件大事：
 *   1. createToken()  — 生成 UUID → 存 Redis → 创建 JWT（登录时调用）
 *   2. getLoginUser() — 解析 JWT → 查 Redis → 返回 LoginUser（每次请求时调用）
 *   3. refreshToken() — 刷新 Redis 过期时间（自动续期）
 *
 * ★ 关键设计：
 *   - JWT 只是一个"信封"，里面只装了一个随机 UUID
 *   - 真正的用户信息（LoginUser）存在 Redis 里
 *   - 这样做的好处：管理员改了权限，不用重新登录就能生效
 *
 * @author share
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private RedisService redisService;
    // Redis 操作服务，用于存取 LoginUser 对象

    protected static final long MILLIS_SECOND = 1000;
    // 1 秒 = 1000 毫秒

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;
    // 1 分 = 60000 毫秒

    private final static long expireTime = CacheConstants.EXPIRATION;
    // token 有效期（分钟），默认 720 分钟 = 12 小时
    // 这个值来自 Nacos 配置

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;
    // Redis key 前缀： "login_tokens:"

    private final static Long MILLIS_MINUTE_TEN = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;
    // token 续期间隔（毫秒），默认 120 分钟
    // 当剩余有效期不足此值时，自动续期

    /**
     * ★ 创建 token（登录流程中最关键的方法）
     *
     * TokenController.login() 中，SysLoginService 校验通过后调用此方法。
     *
     * 执行三部曲：
     *   1. 生成随机 UUID（这就是 Redis 中真正的 key）
     *   2. 把完整的 LoginUser 对象存入 Redis
     *   3. 用 UUID 生成 JWT（发给前端）
     *
     * 存储示例：
     *   Redis:   key = "login_tokens:a1b2c3d4-e5f6-..."
     *            value = LoginUser{userid=1, username="admin", roles=[], ...}
     *            TTL = 720 分钟
     *
     *   JWT:     { user_key: "a1b2c3d4-e5f6-...", user_id: 1, user_name: "admin" }
     *
     *   Cookie:  Admin-Token = "eyJhbGciOiJIUzUxMiJ9...." （JWT）
     *
     * @param loginUser 从数据库查到的完整用户信息
     * @return 返回给前端：{ access_token: "JWT...", expires_in: 720 }
     */
    public Map<String, Object> createToken(LoginUser loginUser)
    {
        // ★ 第 1 步：生成随机 UUID，这就是"真正的 token"
        // 例如："a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        String token = IdUtils.fastUUID();

        Long userId = loginUser.getUserid();      // 用户 ID（如 1）
        String userName = loginUser.getUsername(); // 用户名（如 "admin"）

        // 把 UUID 设到 LoginUser 对象中，后面存 Redis 时会一起存进去
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr());  // 记录登录时的 IP

        // ★ 第 2 步：把 LoginUser 存入 Redis
        // refreshToken() 内部：
        //   redisService.setCacheObject(
        //       "login_tokens:a1b2c3d4-e5f6-...",   ← key = 前缀 + UUID
        //       loginUser,                           ← value = 完整的用户对象
        //       720,                                 ← TTL 分钟
        //       TimeUnit.MINUTES
        //   );
        refreshToken(loginUser);

        // ★ 第 3 步：用 UUID 构建 JWT
        // JWT 里只存三个轻量字段，不存完整的用户信息
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(SecurityConstants.USER_KEY, token);           // UUID → 用于查 Redis
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, userId);   // 用于快速获取
        claimsMap.put(SecurityConstants.DETAILS_USERNAME, userName); // 用于快速获取

        // 组装返回结果
        Map<String, Object> rspMap = new HashMap<String, Object>();
        String jwt = JwtUtils.createToken(claimsMap);  // 生成 JWT 字符串
        rspMap.put("access_token", jwt);               // 发给前端
        rspMap.put("expires_in", expireTime);          // 有效期 720 分钟
        return rspMap;
    }

    /**
     * 从当前请求中获取用户信息
     *
     * 在 HeaderInterceptor 中被调用
     *
     * @return LoginUser（Redis 中的完整用户对象）
     */
    public LoginUser getLoginUser()
    {
        return getLoginUser(ServletUtils.getRequest());
    }

    /**
     * 从指定请求中获取用户信息
     *
     * @param request HTTP 请求（从中取出 Authorization 头）
     * @return LoginUser
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 从请求头 Authorization: Bearer xxx 中取出 JWT
        String token = SecurityUtils.getToken(request);
        return getLoginUser(token);
    }

    /**
     * ★ 通过 JWT 查询 Redis，获取完整的用户信息
     *
     * 执行步骤：
     *   1. 解析 JWT → 取出 user_key（UUID）
     *   2. 拼出 Redis key → "login_tokens:" + UUID
     *   3. 查 Redis → 拿到完整的 LoginUser 对象
     *
     * @param token 前端传来的 JWT 字符串
     * @return LoginUser 对象，token 无效则返回 null
     */
    public LoginUser getLoginUser(String token)
    {
        LoginUser user = null;
        try
        {
            if (StringUtils.isNotEmpty(token))
            {
                // 1. 解析 JWT，取出里面的 user_key（UUID）
                String userkey = JwtUtils.getUserKey(token);

                // 2. 拼出 Redis key = "login_tokens:" + UUID
                // 3. 从 Redis 获取完整的 LoginUser 对象
                user = redisService.getCacheObject(getTokenKey(userkey));
                return user;
            }
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常'{}'", e.getMessage());
        }
        return user;
    }

    /**
     * 设置用户身份信息（刷新 Redis 缓存）
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户缓存（退出登录时调用）
     *
     * 解析 JWT → 取出 UUID → 删除 Redis 中对应的 key
     * 删完后，网关再查 Redis 就找不到这个 token 了，返回 401。
     *
     * @param token 前端的 JWT
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            // 解析 JWT 取出 UUID，删除 Redis key
            String userkey = JwtUtils.getUserKey(token);
            redisService.deleteObject(getTokenKey(userkey));
        }
    }

    /**
     * 验证 token 有效期，自动续期
     *
     * 如果剩余有效期不足 120 分钟，自动刷新 Redis 过期时间。
     * 这样用户持续操作时不会突然被踢下线。
     *
     * @param loginUser 当前登录用户
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();     // 预设的过期时间戳
        long currentTime = System.currentTimeMillis();    // 当前时间
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * ★ 刷新 token 有效期
     *
     * 重新设置 LoginUser 的登录时间和过期时间，
     * 然后写入 Redis（刷新 TTL 重新计时 720 分钟）。
     *
     * @param loginUser 当前登录用户
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);

        // 存入 Redis
        // key = "login_tokens:" + loginUser.getToken()（即 UUID）
        String userKey = getTokenKey(loginUser.getToken());
        // 设置过期时间 720 分钟
        redisService.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 拼出完整的 Redis key
     *
     * @param token UUID
     * @return "login_tokens:" + UUID
     */
    private String getTokenKey(String token)
    {
        return ACCESS_TOKEN + token;
    }
}
