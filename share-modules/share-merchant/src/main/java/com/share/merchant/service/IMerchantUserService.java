package com.share.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.merchant.domain.MerchantUser;
import com.share.system.api.model.LoginUser;

/**
 * 商家登录用户 Service 接口
 *
 * @author share
 */
public interface IMerchantUserService extends IService<MerchantUser> {

    /**
     * 商家登录校验
     *
     * @param username 用户名
     * @param password 密码
     * @return LoginUser（含 merchantId）
     */
    LoginUser login(String username, String password);

    /**
     * 根据用户名获取商家用户
     */
    MerchantUser getByUsername(String username);

    /**
     * 根据商家ID获取商家用户
     */
    MerchantUser getByMerchantId(Long merchantId);

    /**
     * 修改密码
     */
    void updatePassword(Long id, String newPassword);

    /**
     * 创建商家用户（审核通过时自动调用）
     *
     * @param merchantId 商家ID
     * @param username   登录账号（可选，默认 shop_{merchantId}）
     * @param password   密码（可选，默认 123456）
     * @return 新建的商家用户
     */
    MerchantUser createMerchantUser(Long merchantId, String username, String password);
}
