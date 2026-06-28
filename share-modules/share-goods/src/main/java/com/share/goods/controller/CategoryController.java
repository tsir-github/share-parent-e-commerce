package com.share.goods.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.goods.domain.Category;
import com.share.goods.service.ICategoryCacheService;
import com.share.goods.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类Controller（管理员端）
 *
 * @author share
 */
@Tag(name = "商品分类管理")
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController extends BaseController {

    private final ICategoryService categoryService;
    private final ICategoryCacheService categoryCacheService;

    @Operation(summary = "查询分类列表")
    @RequiresPermissions("goods:category:list")
    @GetMapping("/list")
    public TableDataInfo list(Category category) {
        startPage();
        List<Category> list = categoryCacheService.getCategoryListWithCache(category);
        return getDataTable(list);
    }

    @Operation(summary = "获取分类树")
    @RequiresPermissions("goods:category:list")
    @GetMapping("/treeselect")
    public AjaxResult treeselect() {
        return success(categoryCacheService.getCategoryTreeWithCache());
    }

    @Operation(summary = "获取分类详细信息")
    @RequiresPermissions("goods:category:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(categoryService.getById(id));
    }

    @Operation(summary = "新增分类")
    @RequiresPermissions("goods:category:add")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Category category) {
        boolean saved = categoryService.save(category);
        if (saved) categoryCacheService.evictCategoryCache();
        return toAjax(saved);
    }

    @Operation(summary = "修改分类")
    @RequiresPermissions("goods:category:edit")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Category category) {
        boolean updated = categoryService.updateById(category);
        if (updated) categoryCacheService.evictCategoryCache();
        return toAjax(updated);
    }

    @Operation(summary = "删除分类")
    @RequiresPermissions("goods:category:remove")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        boolean removed = categoryService.removeCategory(id);
        if (removed) categoryCacheService.evictCategoryCache();
        return toAjax(removed);
    }
}
