package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.goods.domain.ProductSku;
import com.share.goods.service.IMerchantSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家端 SKU 管理
 *
 * @author share
 */
@Tag(name = "商家端SKU管理")
@RestController
@RequestMapping("/api/v1/merchant/sku")
@RequiredArgsConstructor
public class MerchantSkuController {

    private final IMerchantSkuService merchantSkuService;

    @Operation(summary = "商家SKU列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<ProductSku>> list(ProductSku sku) {
        return R.ok(merchantSkuService.selectMerchantSkuList(sku, SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "按商品ID查询SKU")
    @RequiresLogin
    @GetMapping("/product/{productId}")
    public R<List<ProductSku>> listByProduct(@PathVariable Long productId) {
        return R.ok(merchantSkuService.selectSkuByProductId(productId, SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "商家新增SKU")
    @RequiresLogin
    @PostMapping
    public R<Void> add(@RequestBody ProductSku sku) {
        merchantSkuService.addMerchantSku(sku, SecurityUtils.getMerchantId());
        return R.ok();
    }

    @Operation(summary = "商家修改SKU")
    @RequiresLogin
    @PutMapping("/{id}")
    public R<Void> edit(@PathVariable Long id, @RequestBody ProductSku sku) {
        merchantSkuService.updateMerchantSku(id, sku, SecurityUtils.getMerchantId());
        return R.ok();
    }

    @Operation(summary = "商家删除SKU")
    @RequiresLogin
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        merchantSkuService.deleteMerchantSku(id, SecurityUtils.getMerchantId());
        return R.ok();
    }
}
