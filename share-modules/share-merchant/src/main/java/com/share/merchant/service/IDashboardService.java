package com.share.merchant.service;

import java.util.Map;

/**
 * 商家控制台 Service 接口
 *
 * @author share
 */
public interface IDashboardService {

    /**
     * 获取商家数据概览
     */
    Map<String, Object> getDashboard(Long merchantId);
}
