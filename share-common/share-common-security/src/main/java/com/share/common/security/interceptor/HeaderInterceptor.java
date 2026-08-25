package com.share.common.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.utils.ServletUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.auth.AuthUtil;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.model.LoginUser;

/**
 * 请求头拦截器
 *
 * 这是每个微服务（share-device、share-system 等）处理请求前必经的一步。
 * 网关 AuthFilter 把用户信息塞进了请求头，这里负责取出来。
 *
 * 职责：
 *   1. 从请求头取出网关设置的用户信息（user_id、user_name、user_key）
 *   2. 存到 ThreadLocal（SecurityContextHolder）中
 *   3. 用 user_key 查 Redis 获取完整的 LoginUser 对象
 *   4. 检查 token 是否需要续期
 *   5. 请求完成后清理 ThreadLocal（防止内存泄漏）
 *
 * @author share
 */
public class HeaderInterceptor implements AsyncHandlerInterceptor
{
    @Override
    /**
     * 预处理请求
     *
     * @param request  HTTP 请求对象，用于获取请求头中的用户信息
     * @param response HTTP 响应对象，可用于设置响应状态或头信息（当前未使用）
     * @param handler  被调用的处理器对象，通常为 HandlerMethod，用于判断是否为 Controller 方法
     * @return true 表示继续处理后续拦截器和控制器，false 表示中断请求
     * @throws Exception 处理过程中可能抛出的异常
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        // 非 Controller 方法（如静态资源）不需要处理
        if (!(handler instanceof HandlerMethod))
        {
            return true;
        }

        // ===== 第 1 步：从请求头取出网关设置的基本信息 =====
        // 这些是 AuthFilter 塞进来的
        SecurityContextHolder.setUserId(ServletUtils.getHeader(request, SecurityConstants.DETAILS_USER_ID));
        SecurityContextHolder.setUserName(ServletUtils.getHeader(request, SecurityConstants.DETAILS_USERNAME));
        SecurityContextHolder.setUserKey(ServletUtils.getHeader(request, SecurityConstants.USER_KEY));

        // ===== 第 2 步：从请求头中取出 JWT，查 Redis 拿完整的用户信息 =====
        String token = SecurityUtils.getToken();
        if (StringUtils.isNotEmpty(token))
        {
            // AuthUtil.getLoginUser(token) 内部：
            //   1. JwtUtils.getUserKey(token)    → 解析 JWT，取出 UUID
            //   2. redisService.getCacheObject()  → 用 UUID 查 Redis
            //   3. 返回完整的 LoginUser 对象
            LoginUser loginUser = AuthUtil.getLoginUser(token);
            if (StringUtils.isNotNull(loginUser))
            {
                // ===== 第 3 步：检查是否需要续期 =====
                // 如果剩余有效期 < 120 分钟，自动续期
                AuthUtil.verifyLoginUserExpire(loginUser);

                // ===== 第 4 步：完整的用户信息存入 ThreadLocal =====
                // 将完整的 LoginUser 对象存入 ThreadLocal (SecurityContextHolder 内部维护了一个 ThreadLocal<Map>)
                // key: SecurityConstants.LOGIN_USER ("login_user")
                // value: loginUser 对象
                SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
                
                // 同时更新 ThreadLocal 中的用户ID，确保与 LoginUser 中的信息一致
                // key: SecurityConstants.DETAILS_USER_ID ("user_id")
                // value: 用户ID字符串
                SecurityContextHolder.setUserId(loginUser.getUserid().toString());
                
                // 传播商家ID — 商家登录时 LoginUser 中已设置了 merchantId，
                // 管理员无此值，需判空
                if (loginUser.getMerchantId() != null) {
                    SecurityContextHolder.setMerchantId(loginUser.getMerchantId().toString());
                }
            }
        }
        return true;
    }

    /**
     * 请求完成后清理 ThreadLocal
     *
     * 必须清理！否则 ThreadLocal 中的用户信息会一直存在，
     * 导致下一个请求（可能由不同用户发出）拿到上一个用户的登录信息。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception
    {
        SecurityContextHolder.remove();
    }
}
