package com.share.goods.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.share.common.core.domain.R;
import com.share.common.core.utils.StringUtils;
import com.share.goods.domain.Product;
import com.share.goods.service.IProductCacheService;
import com.share.goods.service.IProductService;
import com.share.goods.service.IUserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C端商品浏览 Controller
 *
 * @author share
 */
@Tag(name = "C端商品浏览")
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductApiController {

    private final IProductService productService;
    private final IProductCacheService productCacheService;
    private final IUserFavoriteService userFavoriteService;

    @Operation(summary = "商品列表")
    @GetMapping("/list")
    public R<List<Product>> list(@RequestParam(required = false) String name,
                                 @RequestParam(required = false) Long categoryId) {
        List<Product> products = productService.selectListedProducts(name, categoryId);
        productService.populateReviewStats(products);
        return R.ok(products);
    }

    @Operation(summary = "商品详情（含收藏状态、评价统计）")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Product product = productCacheService.getProductWithCache(id);
        if (product == null) {
            return R.fail("商品不存在或已下架");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        Long userId = com.share.common.security.utils.SecurityUtils.getUserId();
        result.put("isFavorited", userId != null && userFavoriteService.isFavorited(userId, id));
        productService.fillProductReviewStats(product);
        result.put("avgRating", product.getAvgRating());
        result.put("reviewCount", product.getReviewCount());
        return R.ok(result);
    }

    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public R<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "default") String sortBy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> list = productService.searchProducts(keyword, categoryId, minPrice, maxPrice, sortBy);
        productService.populateReviewStats(list);
        PageInfo<Product> pageInfo = new PageInfo<>(list);
        Map<String, Object> result = new HashMap<>();
        result.put("total", pageInfo.getTotal());
        result.put("rows", pageInfo.getList());
        return R.ok(result);
    }
}
