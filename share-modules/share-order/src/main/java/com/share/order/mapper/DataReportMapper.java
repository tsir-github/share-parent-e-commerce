package com.share.order.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 平台数据报表 Mapper
 *
 * <p>聚合查询各业务表，全部使用 {@code db.table} 跨库语法。
 * 不涉及任何写操作，仅 SELECT。</p>
 *
 * @author share
 */
public interface DataReportMapper {

    /** 交易总览（今日/本周/本月/全部） */
    Map<String, Object> selectOverview();

    /** 订单趋势（按天聚合） */
    List<Map<String, Object>> selectTrend(@Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    /** 商品销售排行 */
    List<Map<String, Object>> selectProductRanking(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("sortBy") String sortBy,
                                                   @Param("topN") Integer topN);

    /** 商家销售排行 */
    List<Map<String, Object>> selectMerchantRanking(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate,
                                                    @Param("topN") Integer topN);

    /** 支付统计 */
    List<Map<String, Object>> selectPaymentStats(@Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);
}
