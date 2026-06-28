package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.ProductSku;

import java.util.List;

/**
 * 商家端 SKU Service 接口
 *
 * @author share
 */
public interface IMerchantSkuService extends IService<ProductSku> {

    /**
     * 查询商家SKU列表
     */
    List<ProductSku> selectMerchantSkuList(ProductSku sku, Long merchantId);

    /**
     * 按商品ID查询SKU（含归属校验）
     */
    List<ProductSku> selectSkuByProductId(Long productId, Long merchantId);

    /**
     * 新增SKU（自动设置商家ID）
     */
    void addMerchantSku(ProductSku sku, Long merchantId);

    /**
     * 修改SKU（含归属校验）
     */
    void updateMerchantSku(Long id, ProductSku sku, Long merchantId);

    /**
     * 删除SKU（含归属校验）
     */
    void deleteMerchantSku(Long id, Long merchantId);
}
