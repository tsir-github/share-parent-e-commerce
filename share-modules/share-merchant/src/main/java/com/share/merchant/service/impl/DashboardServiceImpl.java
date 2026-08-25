package com.share.merchant.service.impl;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.merchant.service.IDashboardService;
import com.share.order.api.RemoteOrderInfoService;
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

    private final RemoteOrderInfoService remoteOrderInfoService;

    @Override
    public Map<String, Object> getDashboard(Long merchantId) {
        R<Map<String, Object>> result = remoteOrderInfoService.getMerchantDashboard(merchantId);
        if (result.getCode() != 200) {
            throw new ServiceException(result.getMsg());
        }
        return result.getData();
    }
}
