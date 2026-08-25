package com.share.goods.service.impl;

import com.share.common.core.constant.CacheConstants;
import com.share.common.redis.service.RedisService;
import com.share.goods.domain.SeckillActivity;
import com.share.goods.domain.vo.SeckillActivityVO;
import com.share.goods.service.ISeckillCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀活动缓存服务 — Cache-Aside
 *
 * <p>缓存秒杀活动列表和详情，TTL 1min ± 20% 随机偏移。
 * 列表是聚合结果，活动状态变更时直接 delete 列表缓存，下次请求重建。
 * 不使用延迟双删（列表是聚合结果，没有"修改"语义）。</p>
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillCacheService implements ISeckillCacheService {

    private final RedisService redisService;

    private static final long TTL_MINUTES = 1;
    private static final double TTL_JITTER = 0.2;

    @SuppressWarnings("unchecked")
    public List<SeckillActivityVO> getSeckillList() {
        try {
            return redisService.getCacheObject(CacheConstants.SECKILL_LIST_KEY);
        } catch (Exception e) {
            log.warn("秒杀列表缓存读取异常，回源查库", e);
            return null;
        }
    }

    public void setSeckillList(List<SeckillActivityVO> list) {
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(CacheConstants.SECKILL_LIST_KEY, list, ttl, TimeUnit.MINUTES);
            log.debug("秒杀列表缓存写入");
        } catch (Exception e) {
            log.warn("秒杀列表缓存写入失败", e);
        }
    }

    public SeckillActivity getSeckillDetail(Long activityId) {
        String cacheKey = CacheConstants.SECKILL_DETAIL_KEY + activityId;
        try {
            return redisService.getCacheObject(cacheKey);
        } catch (Exception e) {
            log.warn("秒杀详情缓存读取异常，回源查库: activityId={}", activityId, e);
            return null;
        }
    }

    public void setSeckillDetail(Long activityId, SeckillActivity activity) {
        String cacheKey = CacheConstants.SECKILL_DETAIL_KEY + activityId;
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(cacheKey, activity, ttl, TimeUnit.MINUTES);
            log.debug("秒杀详情缓存写入: activityId={}", activityId);
        } catch (Exception e) {
            log.warn("秒杀详情缓存写入失败: activityId={}", activityId, e);
        }
    }

    public void evictList() {
        try {
            redisService.deleteObject(CacheConstants.SECKILL_LIST_KEY);
            log.debug("秒杀列表缓存失效");
        } catch (Exception e) {
            log.warn("秒杀列表缓存失效失败", e);
        }
    }

    public void evictDetail(Long activityId) {
        String cacheKey = CacheConstants.SECKILL_DETAIL_KEY + activityId;
        try {
            redisService.deleteObject(cacheKey);
            log.debug("秒杀详情缓存失效: activityId={}", activityId);
        } catch (Exception e) {
            log.warn("秒杀详情缓存失效失败: activityId={}", activityId, e);
        }
    }

    private long ttlWithJitter(long baseMinutes) {
        return (long) (baseMinutes * (0.8 + TTL_JITTER * 2 * Math.random()));
    }
}
