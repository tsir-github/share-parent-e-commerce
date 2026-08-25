package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.merchant.service.IMerchantFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端商家收藏 Controller
 *
 * @author share
 */
@Tag(name = "C端商家收藏")
@RestController
@RequestMapping("/api/v1/merchant/favorite")
@RequiredArgsConstructor
public class MerchantFavoriteController {

    private final IMerchantFavoriteService favoriteService;

    @Operation(summary = "切换收藏状态")
    @RequiresLogin
    @PostMapping("/{merchantId}/toggle")
    public R<Map<String, Object>> toggle(@PathVariable Long merchantId) {
        Long userId = SecurityUtils.getUserId();
        boolean favorited = favoriteService.toggle(userId, merchantId);
        long count = favoriteService.countByMerchantId(merchantId);
        return R.ok(Map.of("favorited", favorited, "count", count));
    }

    @Operation(summary = "查询收藏状态")
    @RequiresLogin
    @GetMapping("/{merchantId}/status")
    public R<Map<String, Object>> status(@PathVariable Long merchantId) {
        Long userId = SecurityUtils.getUserId();
        boolean favorited = favoriteService.isFavorited(userId, merchantId);
        long count = favoriteService.countByMerchantId(merchantId);
        return R.ok(Map.of("favorited", favorited, "count", count));
    }

    @Operation(summary = "收藏列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(favoriteService.selectUserFavorites(SecurityUtils.getUserId()));
    }
}
