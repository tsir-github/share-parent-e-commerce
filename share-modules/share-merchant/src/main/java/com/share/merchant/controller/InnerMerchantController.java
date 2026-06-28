package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.service.IMerchantInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家信息内部 Feign Controller
 *
 * @author share
 */
@Tag(name = "商家内部接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/inner/merchant")
public class InnerMerchantController {

    private final IMerchantInfoService merchantInfoService;

    @Operation(summary = "获取商家信息（Feign）")
    @InnerAuth
    @GetMapping("/get/{merchantId}")
    public R<MerchantInfo> getMerchantById(@PathVariable Long merchantId) {
        return R.ok(merchantInfoService.getById(merchantId));
    }

    @Operation(summary = "校验商家状态（Feign）")
    @InnerAuth
    @GetMapping("/checkStatus/{merchantId}")
    public R<Boolean> checkMerchantStatus(@PathVariable Long merchantId) {
        return R.ok(merchantInfoService.checkMerchantStatus(merchantId));
    }
}
