package com.share.goods.service.impl;

import com.share.common.core.constant.CacheConstants;
import com.share.common.redis.service.RedisService;
import com.share.goods.domain.Category;
import com.share.goods.service.ICategoryCacheService;
import com.share.goods.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 分类缓存服务 — Cache-Aside + 延迟双删
 *
 * @author share
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheServiceImpl implements ICategoryCacheService {

    private final ICategoryService categoryService;
    private final RedisService redisService;

    private static final long TTL_HOURS = 1;
    private static final long DOUBLE_DELETE_DELAY_MS = 500;
    private static final ScheduledExecutorService DELAYED_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cache-category-double-delete");
                t.setDaemon(true);
                return t;
            });

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> getCategoryListWithCache(Category query) {
        String cacheKey = CacheConstants.CATEGORY_LIST_KEY;
        List<Category> cached = redisService.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<Category> list = categoryService.selectCategoryList(query);
        redisService.setCacheObject(cacheKey, list, TTL_HOURS, TimeUnit.HOURS);
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> getCategoryTreeWithCache() {
        String cacheKey = CacheConstants.CATEGORY_TREE_KEY;
        List<Category> cached = redisService.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<Category> tree = categoryService.selectCategoryTree();
        redisService.setCacheObject(cacheKey, tree, TTL_HOURS, TimeUnit.HOURS);
        return tree;
    }

    @Override
    public void evictCategoryCache() {
        redisService.deleteObject(CacheConstants.CATEGORY_TREE_KEY);
        redisService.deleteObject(CacheConstants.CATEGORY_LIST_KEY);
        DELAYED_EXECUTOR.schedule(() -> {
            redisService.deleteObject(CacheConstants.CATEGORY_TREE_KEY);
            redisService.deleteObject(CacheConstants.CATEGORY_LIST_KEY);
            log.debug("分类缓存延迟双删");
        }, DOUBLE_DELETE_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}
