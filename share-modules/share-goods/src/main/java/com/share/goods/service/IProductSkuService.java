package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.api.RemoteGoodsService;
import com.share.goods.domain.ProductSku;

import java.util.List;

/**
 * 商品 SKU Service 接口
 *
 * @author share
 */
public interface IProductSkuService extends IService<ProductSku> {

    /**
     * 查询 SKU 列表
     */
    List<ProductSku> selectSkuList(ProductSku sku);

    /**
     * 根据商品 ID 查询 SKU
     */
    List<ProductSku> selectSkuByProductId(Long productId);

    /**
     * 获取 SKU（含存在性校验）
     *
     * @throws ServiceException 如果 SKU 不存在
     */
    ProductSku getSkuById(Long skuId);

    /**
     * 扣减库存
     */
    boolean deductStock(Long skuId, Integer quantity);

    /**
     * 归还库存
     */
    boolean releaseStock(Long skuId, Integer quantity);

    /**
     * 批量扣减库存（orderNo 用于请求级幂等 → goods 侧 Redis SETNX 防重复扣）
     */
    boolean deductStockBatch(List<RemoteGoodsService.StockDeductDTO> items, String orderNo);

    /**
     * 批量归还库存（orderNo 用于请求级幂等）
     */
    void releaseStockBatch(List<RemoteGoodsService.StockDeductDTO> items, String orderNo);
}
