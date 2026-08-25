package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.goods.domain.vo.HomepageVO;
import com.share.goods.service.IHomepageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端首页 Controller
 *
 * @author share
 */
@Tag(name = "C端首页")
@RestController
@RequestMapping("/api/v1/homepage")
@RequiredArgsConstructor
public class HomepageController {

    private final IHomepageService homepageService;

    @Operation(summary = "首页聚合数据")
    @GetMapping
    public R<HomepageVO> getHomepage() {
        return R.ok(homepageService.getHomepage());
    }

    @Operation(summary = "Banner列表（C端）")
    @GetMapping("/banners")
    public R<HomepageVO> banners() {
        return R.ok(homepageService.getHomepage());
    }
}
