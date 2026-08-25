package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.goods.domain.Category;
import com.share.goods.service.ICategoryCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端分类导航 Controller
 *
 * @author share
 */
@Tag(name = "C端分类导航")
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryApiController {

    private final ICategoryCacheService categoryCacheService;

    @Operation(summary = "分类导航树")
    @GetMapping("/tree")
    public R<List<Category>> tree() {
        return R.ok(categoryCacheService.getCategoryTreeWithCache());
    }
}
