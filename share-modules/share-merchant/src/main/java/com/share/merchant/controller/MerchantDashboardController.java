package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.merchant.service.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商家控制台数据概览
 *
 * @author share
 */
@Tag(name = "商家控制台")
@RestController
@RequestMapping("/api/v1/merchant/dashboard")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final IDashboardService dashboardService;

    @Operation(summary = "商家数据概览")
    @RequiresLogin
    @GetMapping
    public R<Map<String, Object>> dashboard() {
        Long merchantId = SecurityUtils.getMerchantId();
        if (merchantId == null) return R.fail("未获取到商家信息");
        return R.ok(dashboardService.getDashboard(merchantId));
    }
}
