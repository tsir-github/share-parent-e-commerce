package com.share.user.api;

import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.user.domain.UserInfo;
import com.share.user.domain.dto.WxLoginResultDTO;
import com.share.user.service.IUserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserInfoApiController extends BaseController {

    private final IUserInfoService userInfoService;

    //微信授权登录-远程调用
    @Operation(summary = "小程序授权登录")
    @GetMapping("/wxLogin/{code}")
    public R<WxLoginResultDTO> wxLogin(@PathVariable String code) {
        return R.ok(userInfoService.wxLogin(code));
    }

    @Operation(summary = "获取当前登录用户信息")
    @RequiresLogin
    @GetMapping("/getLoginUserInfo")
    public AjaxResult getLoginUserInfo(HttpServletRequest request) {
        Long userId = SecurityContextHolder.getUserId();
        return success(userInfoService.getLoginUserVo(userId));
    }

    @Operation(summary = "获取用户详细信息")
    @GetMapping(value = "/getUserInfo/{id}")
    public R<UserInfo> getInfo(@PathVariable("id") Long id) {
        UserInfo userInfo = userInfoService.getById(id);
        return R.ok(userInfo);
    }

    //统计2024年每个月注册人数
    //远程调用：统计用户注册数据
    @GetMapping("/getUserCount")
    public R<Map<String,Object>> getUserCount() {
        //[150, 230, 224, 218, 135, 147, 260]
        //['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        Map<String,Object> map = userInfoService.getUserCount();
        return R.ok(map);
    }


}
