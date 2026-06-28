package com.share.user.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.user.domain.UserAddress;
import com.share.user.service.IUserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端用户收货地址 Controller
 *
 * @author share
 */
@Tag(name = "C端收货地址")
@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final IUserAddressService userAddressService;

    @Operation(summary = "地址列表")
    @RequiresLogin
    @GetMapping("/list")
    public R<List<UserAddress>> list() {
        return R.ok(userAddressService.selectAddressListByUserId(SecurityUtils.getUserId()));
    }

    @Operation(summary = "新增地址")
    @RequiresLogin
    @PostMapping
    public R<Void> add(@RequestBody UserAddress address) {
        address.setUserId(SecurityUtils.getUserId());
        userAddressService.save(address);
        return R.ok();
    }

    @Operation(summary = "修改地址")
    @RequiresLogin
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody UserAddress address) {
        userAddressService.updateUserAddress(id, SecurityUtils.getUserId(), address);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @RequiresLogin
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        userAddressService.removeUserAddress(id, SecurityUtils.getUserId());
        return R.ok();
    }

    @Operation(summary = "设为默认地址")
    @RequiresLogin
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        userAddressService.setDefault(id, SecurityUtils.getUserId());
        return R.ok();
    }
}
