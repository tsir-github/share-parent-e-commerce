package com.share.common.redis.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 幂等 Token 服务
 *
 * <p>防止网络抖动导致用户重复提交（连续点击"提交订单"等）。
 * 前端获取 Token → 提交时携带 → 后端一次性消费（Redis SETNX 原子校验）。</p>
 *
 * <pre>{@code
 *   // Controller 生成 Token
 *   String token = idempotentService.generateToken();
 *
 *   // Controller 校验 Token
 *   if (!idempotentService.consumeToken(token)) {
 *       return R.fail("请勿重复提交");
 *   }
 * }</pre>
 *
 * @author share
 */
@RequiredArgsConstructor
public class IdempotentService {

    private final RedisService redisService;

    /** 幂等 Token 前缀（与 CacheConstants 保持一致） */
    private static final String TOKEN_PREFIX = "idempotent:token:";
    /** Token 有效期（分钟） */
    private static final long TOKEN_TTL_MINUTES = 5;

    /**
     * 生成一次性幂等 Token
     *
     * <p>写入 Redis，TTL 5min 自动过期（防止泄露后长期有效）。</p>
     */
    public String generateToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        redisService.setCacheObject(key, "1", TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 消费幂等 Token（原子校验 + 删除）
     *
     * <p>调用 Redis DELETE，返回 true = Token 存在且已被消费，false = Token 不存在或已被消费。
     * DELETE 操作本身是原子的，无需额外 SETNX。</p>
     *
     * @param token 前端传入的 Token
     * @return true=首次提交（消费成功） false=重复提交或 Token 无效
     */
    public boolean consumeToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String key = TOKEN_PREFIX + token;
        // Redis DELETE 返回 true 表示 key 存在并被删除（即首次消费）
        return redisService.deleteObject(key);
    }
}
