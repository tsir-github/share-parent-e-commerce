package com.share.merchant.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 商家控制台统计 Mapper
 *
 * <p>跨库查询 share_order 统计数据。
 * MySQL 跨库查询需要在 datasource URL 加 allowMultiQueries=true。</p>
 *
 * @author share
 */
public interface MerchantDashboardMapper {

    /**
     * 查询商家数据概览
     */
    Map<String, Object> selectMerchantDashboard(@Param("merchantId") Long merchantId);
}
