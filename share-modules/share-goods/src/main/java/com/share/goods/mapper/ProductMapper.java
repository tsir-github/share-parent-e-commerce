package com.share.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.goods.domain.Product;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品Mapper接口
 *
 * @author share
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 查询商品列表
     */
    List<Product> selectProductList(Product product);

    /**
     * 搜索商品（C端）
     */
    List<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("categoryId") Long categoryId,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("sortBy") String sortBy);
}
