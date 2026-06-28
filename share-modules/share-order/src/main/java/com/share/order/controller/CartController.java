package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.domain.Cart;
import com.share.order.service.ICartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端购物车 Controller
 *
 * @author share
 */
@Tag(name = "C端购物车")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @Operation(summary = "购物车列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<Cart>> list() {
        return R.ok(cartService.selectCartListByUserId(SecurityUtils.getUserId()));
    }

    @Operation(summary = "加入购物车")
    @RequiresLogin
    @PostMapping("/add")
    public R<Void> add(@RequestParam Long productId,
                       @RequestParam Long skuId,
                       @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.addToCart(SecurityUtils.getUserId(), productId, skuId, quantity);
        return R.ok();
    }

    @Operation(summary = "修改数量/删除")
    @RequiresLogin
    @PutMapping("/{id}")
    public R<Void> updateQuantity(@PathVariable Long id,
                                  @RequestParam Integer quantity) {
        cartService.updateQuantity(id, SecurityUtils.getUserId(), quantity);
        return R.ok();
    }

    @Operation(summary = "切换选中")
    @RequiresLogin
    @PutMapping("/{id}/check")
    public R<Void> toggleCheck(@PathVariable Long id) {
        cartService.toggleChecked(id, SecurityUtils.getUserId());
        return R.ok();
    }
}
