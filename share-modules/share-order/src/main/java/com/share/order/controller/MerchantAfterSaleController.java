package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.AfterSaleRequest;
import com.share.order.service.IMerchantAfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商家端售后审核
 *
 * @author share
 */
@Tag(name = "商家端售后审核")
@RestController
@RequestMapping("/api/v1/merchant/after-sale")
@RequiredArgsConstructor
public class MerchantAfterSaleController {

    private final IMerchantAfterSaleService merchantAfterSaleService;

    @Operation(summary = "商家售后列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<AfterSaleRequest>> list() {
        return R.ok(merchantAfterSaleService.selectMerchantAfterSaleList(SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "商家同意退款")
    @RequiresLogin
    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        merchantAfterSaleService.approveAfterSale(id, SecurityUtils.getMerchantId());
        return R.ok();
    }

    @Operation(summary = "商家拒绝退款（转客服）")
    @RequiresLogin
    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        merchantAfterSaleService.rejectAfterSale(id, SecurityUtils.getMerchantId(), body.get("reason"));
        return R.ok();
    }
}
