package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.goods.domain.UserFavorite;
import com.share.goods.service.IUserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端商品收藏
 *
 * @author share
 */
@Tag(name = "C端商品收藏")
@RestController
@RequestMapping("/api/v1/product/favorite")
@RequiredArgsConstructor
public class UserFavoriteController {

    private final IUserFavoriteService userFavoriteService;

    @Operation(summary = "切换收藏状态")
    @RequiresLogin
    @PostMapping("/toggle/{productId}")
    public R<Map<String, Object>> toggle(@PathVariable Long productId) {
        boolean favorited = userFavoriteService.toggle(SecurityUtils.getUserId(), productId);
        return R.ok(Map.of("favorited", favorited));
    }

    @Operation(summary = "查询是否已收藏")
    @RequiresLogin
    @GetMapping("/check/{productId}")
    public R<Map<String, Object>> check(@PathVariable Long productId) {
        boolean favorited = userFavoriteService.isFavorited(SecurityUtils.getUserId(), productId);
        return R.ok(Map.of("favorited", favorited));
    }

    @Operation(summary = "收藏列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(userFavoriteService.selectUserFavorites(SecurityUtils.getUserId()));
    }
}
