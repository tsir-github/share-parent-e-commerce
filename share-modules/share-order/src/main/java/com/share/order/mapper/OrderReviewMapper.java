package com.share.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.order.domain.OrderReview;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 订单评价 Mapper
 *
 * @author share
 */
public interface OrderReviewMapper extends BaseMapper<OrderReview> {

    /**
     * 批量查询商品评价统计
     */
    @MapKey("productId")
    List<Map<String, Object>> selectReviewStatsByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * 查询单个商品评价统计
     */
    Map<String, Object> selectReviewStatsByProductId(@Param("productId") Long productId);
}
