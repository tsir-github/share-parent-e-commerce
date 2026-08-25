package com.share.auth.service;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.merchant.api.RemoteMerchantUserService;
import com.share.merchant.domain.MerchantUser;
import com.share.system.api.model.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * 商家登录 Service
 *
 * <p>通过 Feign 调用 share-merchant 的 InnerMerchantUserController
 * 完成商家账号密码校验，返回 LoginUser（含 merchantId）。</p>
 *
 * @author share
 */
@Component
@RequiredArgsConstructor
public class MerchantLoginService {

    private final RemoteMerchantUserService remoteMerchantUserService;

    /**
     * 商家登录校验
     *
     * @param username 用户名
     * @param password 密码
     * @return LoginUser（含 merchantId）
     */
    public LoginUser login(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new ServiceException("账号或密码不能为空");
        }

        R<MerchantUser> result = remoteMerchantUserService.login(username, password,
                SecurityConstants.INNER);

        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new ServiceException(result != null ? result.getMsg() : "商家登录失败");
        }

        MerchantUser merchantUser = result.getData();

        // 封装 LoginUser（与管理员登录模型一致）
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(merchantUser.getId());
        loginUser.setUsername(merchantUser.getUsername());
        loginUser.setMerchantId(merchantUser.getMerchantId());
        loginUser.setStatus("0");
        // 商家角色不需要 RuoYi 菜单权限，设为 *:*:* 确保与前端一致、API可调
        HashSet<String> perms = new HashSet<>();
        perms.add("*:*:*");
        loginUser.setPermissions(perms);
        loginUser.setRoles(new HashSet<>());
        return loginUser;
    }
}
