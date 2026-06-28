package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.goods.domain.Product;
import com.share.goods.service.IMerchantProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 商家端商品管理
 *
 * @author share
 */
@Tag(name = "商家端商品管理")
@RestController
@RequestMapping("/api/v1/merchant/product")
@RequiredArgsConstructor
public class MerchantProductController {

    private final IMerchantProductService merchantProductService;

    @Operation(summary = "商家商品列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<Product>> list(Product product) {
        return R.ok(merchantProductService.selectMerchantProductList(product, SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "商家商品详情")
    @RequiresLogin
    @GetMapping("/{id}")
    public R<Product> getInfo(@PathVariable Long id) {
        return R.ok(merchantProductService.getMerchantProduct(id, SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "商家新增商品")
    @RequiresLogin
    @PostMapping
    public R<Void> add(@RequestBody Product product) {
        merchantProductService.addMerchantProduct(product, SecurityUtils.getMerchantId());
        return R.ok();
    }

    @Operation(summary = "商家修改商品")
    @RequiresLogin
    @PutMapping("/{id}")
    public R<Void> edit(@PathVariable Long id, @RequestBody Product product) {
        merchantProductService.updateMerchantProduct(id, product, SecurityUtils.getMerchantId());
        return R.ok();
    }

    @Operation(summary = "商家删除商品")
    @RequiresLogin
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        merchantProductService.deleteMerchantProduct(id, SecurityUtils.getMerchantId());
        return R.ok();
    }

    @Operation(summary = "上传商品图片")
    @RequiresLogin
    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<List<Map<String, Object>>> uploadImages(
            @PathVariable Long productId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false, defaultValue = "detail") String type) {
        if (files == null || files.length == 0) {
            return R.fail("文件不能为空");
        }
        if (files.length > 9) {
            return R.fail("单次最多上传9张图片");
        }
        for (MultipartFile f : files) {
            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                return R.fail("只允许上传图片文件: " + f.getOriginalFilename());
            }
            if (f.getSize() > 10 * 1024 * 1024) {
                return R.fail("图片大小不能超过10MB: " + f.getOriginalFilename());
            }
        }
        return R.ok(merchantProductService.uploadProductImages(productId, files, type, SecurityUtils.getMerchantId()));
    }

    @Operation(summary = "批量上下架")
    @RequiresLogin
    @PutMapping("/batch-status")
    public R<Map<String, Object>> batchStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        String status = (String) body.get("status");
        if (rawIds == null || rawIds.isEmpty()) {
            return R.fail("商品ID列表不能为空");
        }
        if (status == null || (!"0".equals(status) && !"1".equals(status))) {
            return R.fail("状态值无效（0=上架，1=下架）");
        }
        List<Long> ids = rawIds.stream().map(Integer::longValue).toList();
        return R.ok(merchantProductService.batchUpdateStatus(ids, status, SecurityUtils.getMerchantId()));
    }
}
