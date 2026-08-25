package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.goods.api.RemoteGoodsService;
import com.share.goods.domain.ProductSku;
import com.share.goods.service.IProductSkuService;
import com.share.goods.service.ISkuCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品SKU内部接口（Feign 调用）
 *
 * <p>供 share-order 等模块通过 RemoteGoodsService Feign 调用，
 * 所有方法标注 @InnerAuth 仅允许内部 Feign 请求。</p>
 *
 * @author share
 */
@Tag(name = "商品SKU内部接口")
@RestController
@RequestMapping("/inner/sku")
@RequiredArgsConstructor
public class InnerSkuController {

    private final IProductSkuService productSkuService;
    private final ISkuCacheService skuCacheService;

    @Operation(summary = "根据 SKU ID 获取 SKU 信息（内部）")
    @InnerAuth
    @GetMapping("/{skuId}")
    public R<ProductSku> getSkuById(@PathVariable Long skuId) {
        ProductSku cached = skuCacheService.getSku(skuId);
        if (cached != null) {
            return R.ok(cached);
        }
        ProductSku sku = productSkuService.getSkuById(skuId);
        if (sku != null) {
            skuCacheService.setSku(skuId, sku);
        }
        return R.ok(sku);
    }

    @Operation(summary = "批量扣减库存（内部）")
    @InnerAuth
    @PutMapping("/deductStock")
    public R<Boolean> deductStock(@RequestBody List<RemoteGoodsService.StockDeductDTO> items) {
        if (items == null || items.isEmpty()) {
            return R.fail("扣减库存列表为空");
        }
        String orderNo = items.get(0).getOrderNo();
        productSkuService.deductStockBatch(items, orderNo);
        return R.ok(true);
    }

    @Operation(summary = "批量归还库存（内部）")
    @InnerAuth
    @PutMapping("/releaseStock")
    public R<Boolean> releaseStock(@RequestBody List<RemoteGoodsService.StockDeductDTO> items) {
        if (items == null || items.isEmpty()) {
            return R.ok(true);
        }
        String orderNo = items.get(0).getOrderNo();
        productSkuService.releaseStockBatch(items, orderNo);
        return R.ok(true);
    }
}
