package com.share.goods.service;

import com.share.goods.domain.Product;

/**
 * 商品缓存服务接口
 *
 * @author share
 */
public interface IProductCacheService {

    /**
     * Cache-Aside 读商品详情（含 SKU）
     *
     * @param id 商品ID
     * @return 商品对象（含 skus），或 null（已下架/不存在）
     */
    Product getProductWithCache(Long id);

    /**
     * 失效商品缓存（修改/删除时调用）
     *
     * @param id 商品ID
     */
    void evictProductCache(Long id);
}
