package com.share.goods.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.goods.domain.Product;
import com.share.goods.service.IProductCacheService;
import com.share.goods.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品Controller（管理员端）
 *
 * @author share
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController extends BaseController {

    private final IProductService productService;
    private final IProductCacheService productCacheService;

    @Operation(summary = "查询商品列表")
    @RequiresPermissions("goods:product:list")
    @GetMapping("/list")
    public TableDataInfo list(Product product) {
        startPage();
        List<Product> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    @Operation(summary = "查询商品下拉列表（供秒杀等关联使用）")
    @RequiresPermissions("goods:product:list")
    @GetMapping("/selectList")
    public AjaxResult selectList(Product product) {
        List<Product> list = productService.selectProductList(product);
        return success(list);
    }

    @Operation(summary = "获取商品详细信息")
    @RequiresPermissions("goods:product:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(productService.getById(id));
    }

    @Operation(summary = "新增商品")
    @RequiresPermissions("goods:product:add")
    @Log(title = "商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product) {
        return toAjax(productService.save(product));
    }

    @Operation(summary = "修改商品")
    @RequiresPermissions("goods:product:edit")
    @Log(title = "商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Product product) {
        boolean updated = productService.updateById(product);
        if (updated && product.getId() != null) {
            productCacheService.evictProductCache(product.getId());
        }
        return toAjax(updated);
    }

    @Operation(summary = "删除商品")
    @RequiresPermissions("goods:product:remove")
    @Log(title = "商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        boolean removed = productService.removeByIds(List.of(ids));
        if (removed) {
            for (Long id : ids) {
                productCacheService.evictProductCache(id);
            }
        }
        return toAjax(removed);
    }
}
