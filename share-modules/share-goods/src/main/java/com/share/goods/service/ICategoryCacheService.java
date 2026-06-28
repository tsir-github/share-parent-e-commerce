package com.share.goods.service;

import com.share.goods.domain.Category;

import java.util.List;

/**
 * 分类缓存服务接口
 *
 * @author share
 */
public interface ICategoryCacheService {

    /**
     * 获取分类列表（带缓存）
     */
    List<Category> getCategoryListWithCache(Category query);

    /**
     * 获取分类树（带缓存）
     */
    List<Category> getCategoryTreeWithCache();

    /**
     * 失效分类缓存（增删改时调用）
     */
    void evictCategoryCache();
}
