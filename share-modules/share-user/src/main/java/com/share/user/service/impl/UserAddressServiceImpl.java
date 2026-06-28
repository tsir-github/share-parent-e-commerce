package com.share.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.user.domain.UserAddress;
import com.share.user.mapper.UserAddressMapper;
import com.share.user.service.IUserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户收货地址 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements IUserAddressService {

    @Override
    public List<UserAddress> selectAddressListByUserId(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId);
        wrapper.orderByDesc(UserAddress::getIsDefault).orderByDesc(UserAddress::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long userId) {
        UserAddress address = baseMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new ServiceException("地址不存在");
        }
        LambdaUpdateWrapper<UserAddress> unsetWrapper = new LambdaUpdateWrapper<>();
        unsetWrapper.eq(UserAddress::getUserId, userId)
                    .set(UserAddress::getIsDefault, false);
        baseMapper.update(null, unsetWrapper);
        address.setIsDefault(true);
        baseMapper.updateById(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserAddress(Long id, Long userId, UserAddress update) {
        UserAddress existing = baseMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new ServiceException("地址不存在");
        }
        update.setId(id);
        update.setUserId(userId);
        baseMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserAddress(Long id, Long userId) {
        UserAddress existing = baseMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new ServiceException("地址不存在");
        }
        baseMapper.deleteById(id);
    }
}
