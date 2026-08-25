package com.share.goods.service.impl;

import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.MqConstants;
import com.share.common.redis.service.RedisService;
import com.share.goods.domain.Category;
import com.share.goods.service.ICategoryCacheService;
import com.share.goods.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分类缓存服务 — Cache-Aside + MQ 延迟双删
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheServiceImpl implements ICategoryCacheService {

    private final ICategoryService categoryService;
    private final RedisService redisService;
    private final RocketMQTemplate rocketMQTemplate;

    private static final long TTL_MINUTES = 5;
    private static final double TTL_JITTER = 0.2;

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> getCategoryListWithCache(Category query) {
        String cacheKey = CacheConstants.CATEGORY_LIST_KEY;
        try {
            List<Category> cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("分类列表缓存读取异常，回源查库", e);
        }
        List<Category> list = categoryService.selectCategoryList(query);
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(cacheKey, list, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("分类列表缓存写入失败", e);
        }
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> getCategoryTreeWithCache() {
        String cacheKey = CacheConstants.CATEGORY_TREE_KEY;
        try {
            List<Category> cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("分类树缓存读取异常，回源查库", e);
        }
        List<Category> tree = categoryService.selectCategoryTree();
        try {
            long ttl = ttlWithJitter(TTL_MINUTES);
            redisService.setCacheObject(cacheKey, tree, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("分类树缓存写入失败", e);
        }
        return tree;
    }

    @Override
    public void evictCategoryCache() {
        // 第一次删：立即失效，失败抛异常 → 事务回滚
        redisService.deleteObject(CacheConstants.CATEGORY_TREE_KEY);
        redisService.deleteObject(CacheConstants.CATEGORY_LIST_KEY);
        // 第二次删：MQ 延迟消息兜底（最佳努力，失败不影响事务）
        try {
            rocketMQTemplate.syncSend(MqConstants.CACHE_INVALIDATE_TOPIC,
                    MessageBuilder.withPayload(CacheConstants.CATEGORY_TREE_KEY).build(),
                    3000, 3);
            rocketMQTemplate.syncSend(MqConstants.CACHE_INVALIDATE_TOPIC,
                    MessageBuilder.withPayload(CacheConstants.CATEGORY_LIST_KEY).build(),
                    3000, 3);
        } catch (Exception e) {
            log.warn("MQ缓存失效投递失败", e);
        }
    }

    private long ttlWithJitter(long baseMinutes) {
        return (long) (baseMinutes * (0.8 + TTL_JITTER * 2 * Math.random()));
    }
}
