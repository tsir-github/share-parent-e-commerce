package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 商家端商品 Service 接口
 *
 * @author share
 */
public interface IMerchantProductService extends IService<Product> {

    /**
     * 查询商家商品列表
     */
    List<Product> selectMerchantProductList(Product product, Long merchantId);

    /**
     * 查询商品详情（含归属校验）
     */
    Product getMerchantProduct(Long id, Long merchantId);

    /**
     * 新增商品（自动设置商家ID）
     */
    Long addMerchantProduct(Product product, Long merchantId);

    /**
     * 修改商品（含归属校验）
     */
    void updateMerchantProduct(Long id, Product product, Long merchantId);

    /**
     * 删除商品（含归属校验）
     */
    void deleteMerchantProduct(Long id, Long merchantId);

    /**
     * 上传商品图片
     *
     * @param productId  商品ID
     * @param files      图片文件数组
     * @param type       main=主图, detail=详情图
     * @param merchantId 商家ID（归属校验）
     * @return 图片URL列表（含sortOrder）
     */
    List<Map<String, Object>> uploadProductImages(Long productId, MultipartFile[] files, String type, Long merchantId);

    /**
     * 批量上下架商品
     *
     * @param ids        商品ID列表
     * @param status     目标状态：LISTED 上架 / UNLISTED 下架
     * @param merchantId 商家ID（归属校验）
     * @return {success: 成功数, failed: 失败数, errors: [{id, reason}]}
     */
    Map<String, Object> batchUpdateStatus(List<Long> ids, String status, Long merchantId);
}
