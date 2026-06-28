package com.share.merchant.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.service.IMerchantUserService;
import com.share.system.api.model.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家用户内部 Feign Controller
 *
 * @author share
 */
@Tag(name = "商家用户内部接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/inner/merchant/user")
public class InnerMerchantUserController {

    private final IMerchantUserService merchantUserService;

    @Operation(summary = "商家登录校验（Feign）")
    @InnerAuth
    @PostMapping("/login")
    public R<LoginUser> login(@RequestParam String username, @RequestParam String password) {
        LoginUser loginUser = merchantUserService.login(username, password);
        return R.ok(loginUser);
    }

    @Operation(summary = "根据商家ID获取用户信息（Feign）")
    @InnerAuth
    @GetMapping("/getByMerchantId/{merchantId}")
    public R<MerchantUser> getByMerchantId(@PathVariable Long merchantId) {
        MerchantUser merchantUser = merchantUserService.getByMerchantId(merchantId);
        return R.ok(merchantUser);
    }
}
