package com.share.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.goods.domain.ProductSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品SKUMapper接口
 *
 * @author share
 */
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 查询SKU列表
     */
    List<ProductSku> selectSkuList(ProductSku sku);

    /**
     * 乐观锁扣减库存（带 version 校验）
     *
     * @return 影响行数（0 表示版本冲突或库存不足）
     */
    @Update("update product_sku set stock = stock - #{quantity}, sales = sales + #{quantity}, " +
            "version = version + 1 " +
            "where id = #{skuId} and version = #{version} and stock >= #{quantity} and del_flag = 0")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    /**
     * 乐观锁归还库存（取消订单回滚）
     *
     * @return 影响行数（0 表示版本冲突）
     */
    @Update("update product_sku set stock = stock + #{quantity}, sales = sales - #{quantity}, " +
            "version = version + 1 " +
            "where id = #{skuId} and version = #{version} and del_flag = 0")
    int releaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity, @Param("version") Integer version);
}
