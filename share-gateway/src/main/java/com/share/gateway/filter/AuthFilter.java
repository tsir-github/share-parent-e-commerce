package com.share.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.TokenConstants;
import com.share.common.core.utils.JwtUtils;
import com.share.common.core.utils.ServletUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.redis.service.RedisService;
import com.share.gateway.config.properties.IgnoreWhiteProperties;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

/**
 * 网关鉴权过滤器
 *
 * 这是"请求来了先验票"的关卡。
 * 每个请求到达微服务之前，先经过网关，再经过此过滤器。
 *
 * 职责：
 *   1. 白名单路径（如 /auth/login）→ 直接放行
 *   2. 需要登录的路径 → 验证 JWT 签名 + 查 Redis 看 token 是否有效
 *   3. 验证通过后 → 把用户 ID/用户名 塞入请求头，转发给下游微服务
 *
 * 为什么不在这里查 Redis 拿完整的 LoginUser？
 *   网关是流量入口，性能敏感。
 *   用 hasKey() 只判断 key 存在与否，比 getCacheObject() 反序列化整个对象快得多。
 *   完整的用户信息由下游微服务的 HeaderInterceptor 去查。
 *
 * @author share
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Autowired
    private IgnoreWhiteProperties ignoreWhite;
    // Nacos 中配置的白名单路径列表

    @Autowired
    private RedisService redisService;
    // Redis 服务，用于验证 token 是否存在


    @Override
    /**
     * ★ 网关鉴权核心逻辑
     *
     * 每个需要登录的请求都会经过这里，按顺序做 6 件事：
     *   1. 检查是否证在白名单中（登录/注册/验码等路径直接放行）
     *   2. 从 Authorization 请求头取出 JWT
     *   3. 解析 JWT，验证签名是否正确
     *   4. 从 JWT 中取出 user_key（UUID），查 Redis 看 key 还在不在
     *   5. 从 JWT 中取出 user_id 和 user_name
     *   6. 把用户信息塞入请求头，转发给下游微服务
     *
     * @param exchange 服务器交换对象
     * @param chain 过滤器链
     * @return Mono<Void>
     */
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();

        String url = request.getURI().getPath();  // 当前请求路径

        // ===== 第 1 步：白名单检查 =====
        // 登录/注册/验证码等路径直接放行
        if (StringUtils.matches(url, ignoreWhite.getWhites()))
        {
            return chain.filter(exchange);
        }

        // ===== 第 2 步：取 JWT =====
        String token = getToken(request);
        if (StringUtils.isEmpty(token))
        {
            return unauthorizedResponse(exchange, "令牌不能为空");
        }

        // ===== 第 3 步：解析 JWT，验证签名 =====
        Claims claims = JwtUtils.parseToken(token);
        if (claims == null)
        {
            return unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
        }

        // ===== 第 4 步：从 JWT 取 user_key（UUID），查 Redis =====
        String userkey = JwtUtils.getUserKey(claims);
        // hasKey 比 getCacheObject 快：
        // 只返回 true/false，不需要反序列化整个 LoginUser 对象
        boolean islogin = redisService.hasKey(getTokenKey(userkey));
        if (!islogin)
        {
            return unauthorizedResponse(exchange, "登录状态已过期");
        }

        // ===== 第 5 步：取用户基本信息 =====
        String userid = JwtUtils.getUserId(claims);
        String username = JwtUtils.getUserName(claims);
        if (StringUtils.isEmpty(userid) || StringUtils.isEmpty(username))
        {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }

        // ===== 第 6 步：用户信息塞入请求头，转发给下游微服务 =====
        addHeader(mutate, SecurityConstants.USER_KEY, userkey);
        addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userid);
        addHeader(mutate, SecurityConstants.DETAILS_USERNAME, username);
        addHeader(mutate, SecurityConstants.DETAILS_MERCHANT_ID, JwtUtils.getMerchantId(claims));
        // 清除内部调用标记，防止伪造内部请求
        removeHeader(mutate, SecurityConstants.FROM_SOURCE);
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value)
    {
        if (value == null)
        {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = ServletUtils.urlEncode(valueStr);
        mutate.header(name, valueEncode);
    }

    private void removeHeader(ServerHttpRequest.Builder mutate, String name)
    {
        mutate.headers(httpHeaders -> httpHeaders.remove(name)).build();
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg)
    {
        log.error("[鉴权异常处理]请求路径:{}", exchange.getRequest().getPath());
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), msg, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 拼出 Redis key
     * key = "login_tokens:" + user_key（UUID）
     */
    private String getTokenKey(String token)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 从请求头中取出 JWT
     *
     * 前端发的请求头格式：
     *   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
     * 这个方法会去掉 "Bearer " 前缀，只返回 JWT 本体
     */
    private String getToken(ServerHttpRequest request)
    {
        String token = request.getHeaders().getFirst(TokenConstants.AUTHENTICATION);
        // 如果前端设置了令牌前缀（"Bearer "），则裁剪掉
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX))
        {
            token = token.replaceFirst(TokenConstants.PREFIX, StringUtils.EMPTY);
        }
        return token;
    }

    /**
     * 设置过滤器优先级
     * 数字越小越先执行
     */
    @Override
    public int getOrder()
    {
        return -200;
    }
}
