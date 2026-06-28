package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 Service 接口
 *
 * @author share
 */
public interface IProductService extends IService<Product> {

    /**
     * 查询商品列表
     */
    List<Product> selectProductList(Product product);

    /**
     * 查询上架商品列表（C端浏览）
     */
    List<Product> selectListedProducts(String name, Long categoryId);

    /**
     * 获取上架商品详情（C端浏览，含缓存）
     *
     * @return 商品，或 null（不存在/已下架）
     */
    Product getListedProduct(Long id);

    /**
     * 搜索商品（C端）
     *
     * @param keyword    关键词（匹配 name + subtitle）
     * @param categoryId 分类ID（可选）
     * @param minPrice   最低价（可选）
     * @param maxPrice   最高价（可选）
     * @param sortBy     排序方式：default/sales/price_asc/price_desc
     * @return 商品列表（PageHelper 已分页）
     */
    List<Product> searchProducts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy);

    /**
     * 批量填充商品评价统计（avgRating, reviewCount）
     */
    void populateReviewStats(List<Product> products);

    /**
     * 填充单个商品评价统计
     */
    void fillProductReviewStats(Product product);
}
