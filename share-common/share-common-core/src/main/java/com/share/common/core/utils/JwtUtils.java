package com.share.common.core.utils;

import java.util.Map;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.TokenConstants;
import com.share.common.core.text.Convert;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Jwt 工具类
 *
 * 负责两件事：
 * 1. 生成 JWT（在 TokenService.createToken() 中调用）
 * 2. 解析 JWT（在网关 AuthFilter 和下游 HeaderInterceptor 中调用）
 *
 * JWT 在本系统中的作用是"信封"而非"车票"：
 * - 车票模式：JWT 里存完整用户信息，直接解析就能用，不查 Redis
 * - 信封模式（本系统采用）：JWT 里只存一个 UUID，真正的用户信息在 Redis 里
 *
 * 为啥用信封模式？
 * 如果权限变了（管理员给你加了新权限），存在 Redis 里下次请求就能生效。
 * 如果写在 JWT 里，必须重新登录才能拿到新的 JWT。
 *
 * @author share
 */
public class JwtUtils
{
    /**
     * 签名密钥（从 TokenConstants.SECRET 读取）
     * 这个密钥只有后端知道，用于签发和验证 JWT 的签名，
     * 保证 JWT 里的内容没有被篡改过。
     */
    public static String secret = TokenConstants.SECRET;

    /**
     * 生成 JWT
     *
     * TokenService.createToken() 中调用此方法，
     * 把 user_key（UUID）、user_id、user_name 三个字段打包成 JWT。
     *
     * 生成的 JWT 格式：
     *   Header: {"alg":"HS512"}                          ← 加密算法
     *   Payload: {"user_key":"UUID","user_id":1,"user_name":"admin"}  ← 实际数据
     *   Signature: 用 secret 对 Header+Payload 签名        ← 防篡改
     *
     * 三部分用 . 拼接，最终形如：eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2tleSI6ImExYjJjM2Q0...签名...
     *
     * @param claims 要存入 JWT 的数据（user_key、user_id、user_name）
     * @return JWT 字符串（前端 Cookie 里存的就是这个）
     */
    public static String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 解析 JWT，取出里面的数据声明
     *
     * 网关 AuthFilter 中调用此方法：
     *   解析出 user_key → 查 Redis 验证 token 是否有效
     *   解析出 user_id、user_name → 塞入请求头传给下游
     *
     * 如果 JWT 被篡改过，或者已过期，这里会抛出异常。
     *
     * @param token 前端 Authorization 头里带的 JWT
     * @return JWT 的 Payload 部分（Claims 对象）
     */
    public static Claims parseToken(String token)
    {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 从 JWT 中取出 user_key（UUID）
     *
     * user_key 是 TokenService.createToken() 中生成的随机 UUID，
     * 它是 Redis 中存储 LoginUser 的 key 的一部分。
     *
     * 完整的 Redis key = "login_tokens:" + user_key
     *
     * @param token 前端传来的 JWT
     * @return UUID 字符串，用于查 Redis
     */
    public static String getUserKey(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 从已解析的 Claims 中取出 user_key
     *
     * @param claims 已解析好的 JWT 数据
     * @return UUID 字符串
     */
    public static String getUserKey(Claims claims)
    {
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 从 JWT 中取出用户 ID
     *
     * 网关 AuthFilter 中用这个拿到 userid，
     * 塞进请求头传到下游微服务。
     *
     * @param token 前端传来的 JWT
     * @return 用户 ID（如 "1"）
     */
    public static String getUserId(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.DETAILS_USER_ID);
    }

    /**
     * 从已解析的 Claims 中取出用户 ID
     *
     * @param claims 已解析好的 JWT 数据
     * @return 用户 ID 字符串
     */
    public static String getUserId(Claims claims)
    {
        return getValue(claims, SecurityConstants.DETAILS_USER_ID);
    }

    /**
     * 从 JWT 中取出用户名
     *
     * @param token 前端传来的 JWT
     * @return 用户名（如 "admin"）
     */
    public static String getUserName(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.DETAILS_USERNAME);
    }

    /**
     * 从已解析的 Claims 中取出用户名
     *
     * @param claims 已解析好的 JWT 数据
     * @return 用户名
     */
    public static String getUserName(Claims claims)
    {
        return getValue(claims, SecurityConstants.DETAILS_USERNAME);
    }

    /**
     * 从 Claims 中安全地取出指定 key 的值
     *
     * @param claims JWT 的 Payload
     * @param key 要取值的键名
     * @return 值，不存在则返回空字符串
     */
    public static String getValue(Claims claims, String key)
    {
        return Convert.toStr(claims.get(key), "");
    }
}
