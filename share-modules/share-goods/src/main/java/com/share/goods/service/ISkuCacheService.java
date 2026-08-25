package com.share.goods.service;

import com.share.goods.domain.ProductSku;

/**
 * SKU 信息缓存服务接口
 *
 * @author share
 */
public interface ISkuCacheService {

    ProductSku getSku(Long skuId);

    void setSku(Long skuId, ProductSku sku);

    void evictSku(Long skuId);
}
