package com.share.goods.service.impl;

import com.share.goods.service.IRedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis Lua 库存预扣服务实现
 *
 * <p>使用 Redis Lua 脚本保证预扣操作的原子性。
 * 扣减前先检查 Redis 中的库存是否足够，足够则扣减。</p>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockServiceImpl implements IRedisStockService {

    private static final String STOCK_KEY_PREFIX = "stock:sku:";
    private static final int STOCK_TTL_HOURS = 24;

    private final RedissonClient redissonClient;
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * Lua 脚本：原子预扣库存
     *
     * KEYS[1] = stock key
     * ARGV[1] = quantity
     *
     * 返回 1 = 扣减成功，0 = 库存不足
     */
    private static final String PRE_DEDUCT_LUA =
            "local key = KEYS[1] " +
            "local qty = tonumber(ARGV[1]) " +
            "local stock = tonumber(redis.call('get', key) or '0') " +
            "if stock >= qty then " +
            "    redis.call('decrby', key, qty) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * Lua 脚本：原子归还库存
     *
     * KEYS[1] = stock key
     * ARGV[1] = quantity
     */
    private static final String PRE_RELEASE_LUA =
            "local key = KEYS[1] " +
            "local qty = tonumber(ARGV[1]) " +
            "redis.call('incrby', key, qty) " +
            "return 1";

    @Override
    public boolean preDeduct(Long skuId, int quantity) {
        String key = STOCK_KEY_PREFIX + skuId;
        // ponytail: 缓存未命中时依赖 DB 乐观锁兜底，不在此处同步
        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                PRE_DEDUCT_LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                quantity
        );
        return result != null && result == 1L;
    }

    @Override
    public void preRelease(Long skuId, int quantity) {
        String key = STOCK_KEY_PREFIX + skuId;
        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        script.eval(
                RScript.Mode.READ_WRITE,
                PRE_RELEASE_LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                quantity
        );
        log.debug("Redis 归还库存: skuId={}, qty={}", skuId, quantity);
    }

    @Override
    public void syncStock(Long skuId, int stock) {
        String key = STOCK_KEY_PREFIX + skuId;
        redisTemplate.opsForValue().set(key, stock, STOCK_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Redis 同步库存: skuId={}, stock={}", skuId, stock);
    }
}
