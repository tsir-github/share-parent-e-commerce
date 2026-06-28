package com.share.common.core.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.text.Convert;
import com.share.common.core.utils.StringUtils;

/**
 * 当前线程的"用户上下文"容器
 *
 * 基于 ThreadLocal 实现，每个请求对应一个线程。
 * 在请求处理期间，任意位置都可以通过这个工具类获取当前登录用户的信息。
 *
 * 数据来源（按设置顺序）：
 *   1. 网关 AuthFilter 从 JWT 解析出 user_id、user_name，塞入请求头
 *   2. 微服务的 HeaderInterceptor 从请求头取出，通过此类的 setXxx() 方法存入 ThreadLocal
 *   3. HeaderInterceptor 再从 Redis 查出完整的 LoginUser 对象，一起存进来
 *
 * 生命周期：
 *   HeaderInterceptor.preHandle()     → 设置（请求开始）
 *   HeaderInterceptor.afterCompletion() → 清理（请求结束）
 *   同一个线程内，任意代码都可以通过 getUserId()、getUserName() 获取当前用户。
 *
 * @author share
 */
public class SecurityContextHolder
{
    // ★ TransmittableThreadLocal 是阿里开源的 ThreadLocal 增强版
    // 普通 ThreadLocal 在异步线程中无法传递值
    // TTL 可以在线程池的父子线程之间传递数据（比如异步处理日志时也能拿到用户信息）
    private static final TransmittableThreadLocal<Map<String, Object>> THREAD_LOCAL = new TransmittableThreadLocal<Map<String, Object>>();

    /**
     * 存入一个键值对到当前线程的上下文中
     *
     * @param key   键（如 "login_user"、"user_id"）
     * @param value 值
     */
    public static void set(String key, Object value)
    {
        Map<String, Object> map = getLocalMap();
        map.put(key, value == null ? StringUtils.EMPTY : value);
    }

    /**
     * 从当前线程上下文中获取一个字符串值
     *
     * @param key 键
     * @return 值（不存在返回空字符串）
     */
    public static String get(String key)
    {
        Map<String, Object> map = getLocalMap();
        return Convert.toStr(map.getOrDefault(key, StringUtils.EMPTY));
    }

    /**
     * 从当前线程上下文中获取一个指定类型的值
     *
     * @param key   键
     * @param clazz 类型
     * @return 值（不存在返回 null）
     */
    public static <T> T get(String key, Class<T> clazz)
    {
        Map<String, Object> map = getLocalMap();
        return StringUtils.cast(map.getOrDefault(key, null));
    }

    /**
     * 获取当前线程的 Map（如果不存在则创建）
     */
    public static Map<String, Object> getLocalMap()
    {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null)
        {
            map = new ConcurrentHashMap<String, Object>();
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    /**
     * 直接设置整个 Map 到当前线程
     */
    public static void setLocalMap(Map<String, Object> threadLocalMap)
    {
        THREAD_LOCAL.set(threadLocalMap);
    }

    /**
     * ★ 获取当前登录的用户 ID
     *
     * 在你的 Service/Controller 中可以直接调用：
     *   Long currentUserId = SecurityContextHolder.getUserId();
     *
     * @return 用户 ID（如 1），获取不到返回 0L
     */
    public static Long getUserId()
    {
        return Convert.toLong(get(SecurityConstants.DETAILS_USER_ID), 0L);
    }

    /**
     * 设置当前登录的用户 ID
     */
    public static void setUserId(String account)
    {
        set(SecurityConstants.DETAILS_USER_ID, account);
    }

    /**
     * 获取当前登录的用户名
     *
     * @return 用户名（如 "admin"）
     */
    public static String getUserName()
    {
        return get(SecurityConstants.DETAILS_USERNAME);
    }

    /**
     * 设置当前登录的用户名
     */
    public static void setUserName(String username)
    {
        set(SecurityConstants.DETAILS_USERNAME, username);
    }

    /**
     * 获取当前登录的商家ID
     */
    public static Long getMerchantId()
    {
        return Convert.toLong(get(SecurityConstants.DETAILS_MERCHANT_ID), 0L);
    }

    /**
     * 设置当前登录的商家ID
     */
    public static void setMerchantId(String merchantId)
    {
        set(SecurityConstants.DETAILS_MERCHANT_ID, merchantId);
    }

    /**
     * 获取当前线程的 user_key（UUID，用于查 Redis）
     */
    public static String getUserKey()
    {
        return get(SecurityConstants.USER_KEY);
    }

    /**
     * 设置当前线程的 user_key
     */
    public static void setUserKey(String userKey)
    {
        set(SecurityConstants.USER_KEY, userKey);
    }

    /**
     * 获取当前线程的权限标识
     */
    public static String getPermission()
    {
        return get(SecurityConstants.ROLE_PERMISSION);
    }

    /**
     * 设置当前线程的权限标识
     */
    public static void setPermission(String permissions)
    {
        set(SecurityConstants.ROLE_PERMISSION, permissions);
    }

    /**
     * ★ 清理当前线程的上下文
     *
     * 必须在请求结束时调用！
     * 如果忘记清理，线程池复用时下一个请求可能拿到上一个用户的信息，造成越权。
     */
    public static void remove()
    {
        THREAD_LOCAL.remove();
    }
}
