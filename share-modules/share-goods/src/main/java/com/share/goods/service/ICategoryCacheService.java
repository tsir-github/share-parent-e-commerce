package com.share.goods.service;

import com.share.goods.domain.Category;

import java.util.List;

/**
 * 分类缓存服务接口
 *
 * <p>非实体 CRUD 工具类，不继承 {@code IService}。
 * 职责是缓存管理（查缓存→失效缓存），不涉及数据库 CRUD。</p>
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
