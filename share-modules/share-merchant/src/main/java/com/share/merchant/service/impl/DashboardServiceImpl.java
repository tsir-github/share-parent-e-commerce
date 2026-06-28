package com.share.merchant.service.impl;

import com.share.merchant.mapper.MerchantDashboardMapper;
import com.share.merchant.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 商家控制台 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final MerchantDashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> getDashboard(Long merchantId) {
        return dashboardMapper.selectMerchantDashboard(merchantId);
    }
}
