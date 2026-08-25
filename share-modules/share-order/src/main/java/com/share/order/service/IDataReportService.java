package com.share.order.service;

import java.util.List;
import java.util.Map;

/**
 * 平台数据报表 Service 接口
 *
 * <p>非实体 CRUD 工具类，不继承 {@code IService}。
 * 职责是跨库聚合查询（交易总览/趋势/排行），不是单一实体的增删改查。</p>
 *
 * @author share
 */
public interface IDataReportService {

    /** 交易总览 */
    Map<String, Object> getOverview(String startDate, String endDate);

    /** 订单趋势 */
    List<Map<String, Object>> getTrend(String startDate, String endDate);

    /** 商品销售排行 */
    List<Map<String, Object>> getProductRanking(String startDate, String endDate, String sortBy, Integer topN);

    /** 商家销售排行 */
    List<Map<String, Object>> getMerchantRanking(String startDate, String endDate, Integer topN);

    /** 支付统计 */
    List<Map<String, Object>> getPaymentStats(String startDate, String endDate);
}
