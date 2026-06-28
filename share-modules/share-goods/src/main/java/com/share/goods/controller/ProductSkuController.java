package com.share.goods.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.goods.domain.ProductSku;
import com.share.goods.service.IProductSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品SKUController
 *
 * @author share
 */
@Tag(name = "商品SKU管理")
@RestController
@RequestMapping("/sku")
@RequiredArgsConstructor
public class ProductSkuController extends BaseController {

    private final IProductSkuService productSkuService;

    /**
     * 查询SKU列表
     */
    @Operation(summary = "查询SKU列表")
    @RequiresPermissions("goods:sku:list")
    @GetMapping("/list")
    public TableDataInfo list(ProductSku sku) {
        startPage();
        List<ProductSku> list = productSkuService.selectSkuList(sku);
        return getDataTable(list);
    }

    /**
     * 根据商品ID查询SKU
     */
    @Operation(summary = "根据商品ID查询SKU")
    @GetMapping("/product/{productId}")
    public AjaxResult listByProduct(@PathVariable Long productId) {
        return success(productSkuService.selectSkuByProductId(productId));
    }

    /**
     * 获取SKU详细信息
     */
    @Operation(summary = "获取SKU详细信息")
    @RequiresPermissions("goods:sku:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(productSkuService.getById(id));
    }

    /**
     * 新增SKU
     */
    @Operation(summary = "新增SKU")
    @RequiresPermissions("goods:sku:add")
    @Log(title = "商品SKU", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProductSku sku) {
        return toAjax(productSkuService.save(sku));
    }

    /**
     * 修改SKU
     */
    @Operation(summary = "修改SKU")
    @RequiresPermissions("goods:sku:edit")
    @Log(title = "商品SKU", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProductSku sku) {
        return toAjax(productSkuService.updateById(sku));
    }

    /**
     * 删除SKU
     */
    @Operation(summary = "删除SKU")
    @RequiresPermissions("goods:sku:remove")
    @Log(title = "商品SKU", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(productSkuService.removeById(id));
    }
}
