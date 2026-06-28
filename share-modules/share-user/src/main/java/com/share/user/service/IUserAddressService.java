package com.share.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.user.domain.UserAddress;

import java.util.List;

/**
 * 用户收货地址 Service 接口
 *
 * @author share
 */
public interface IUserAddressService extends IService<UserAddress> {

    /**
     * 查询用户地址列表
     */
    List<UserAddress> selectAddressListByUserId(Long userId);

    /**
     * 设置默认地址
     */
    void setDefault(Long id, Long userId);

    /**
     * 更新地址（含归属校验）
     */
    void updateUserAddress(Long id, Long userId, UserAddress update);

    /**
     * 删除地址（含归属校验）
     */
    void removeUserAddress(Long id, Long userId);
}
