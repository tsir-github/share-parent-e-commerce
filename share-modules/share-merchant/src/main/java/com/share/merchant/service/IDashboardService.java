package com.share.merchant.service;

import java.util.Map;

/**
 * 商家控制台 Service 接口
 *
 * <p>非实体 CRUD 工具类，不继承 {@code IService}。
 * 职责是聚合查询（跨库统计），不是单一实体的增删改查。</p>
 *
 * @author share
 */
public interface IDashboardService {

    /**
     * 获取商家数据概览
     */
    Map<String, Object> getDashboard(Long merchantId);
}
