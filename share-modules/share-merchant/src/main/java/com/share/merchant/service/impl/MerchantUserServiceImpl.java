package com.share.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.constant.UserConstants;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.utils.SecurityUtils;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.mapper.MerchantUserMapper;
import com.share.merchant.service.IMerchantInfoService;
import com.share.merchant.service.IMerchantUserService;
import com.share.system.api.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import com.share.common.core.utils.ServletUtils;
import com.share.common.core.utils.ip.IpUtils;

/**
 * 商家登录用户 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class MerchantUserServiceImpl extends ServiceImpl<MerchantUserMapper, MerchantUser> implements IMerchantUserService {

    private final IMerchantInfoService merchantInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantUser createMerchantUser(Long merchantId, String username, String password) {
        MerchantUser user = new MerchantUser();
        user.setMerchantId(merchantId);
        // ponytail: username = shop_{merchantId} if not provided, password = default 123456
        if (StringUtils.isEmpty(username)) {
            username = "shop_" + merchantId;
        }
        if (StringUtils.isEmpty(password)) {
            password = "123456";
        }
        user.setUsername(username);
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setStatus(UserConstants.NORMAL);
        baseMapper.insert(user);
        return user;
    }

    @Override
    public LoginUser login(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new ServiceException("账号或密码不能为空");
        }

        // 查询商家用户
        MerchantUser merchantUser = getByUsername(username);
        if (merchantUser == null) {
            throw new ServiceException("账号或密码错误");
        }

        // 校验密码
        if (!SecurityUtils.matchesPassword(password, merchantUser.getPassword())) {
            throw new ServiceException("账号或密码错误");
        }

        // 校验用户状态
        if (UserConstants.USER_DISABLE.equals(merchantUser.getStatus())) {
            throw new ServiceException("账号已被停用");
        }

        // 校验店铺状态
        if (!merchantInfoService.checkMerchantStatus(merchantUser.getMerchantId())) {
            throw new ServiceException("店铺未审核通过或已关闭");
        }

        // 更新登录信息
        HttpServletRequest request = ServletUtils.getRequest();
        String ip = IpUtils.getIpAddr(request);
        baseMapper.update(null, new LambdaUpdateWrapper<MerchantUser>()
                .eq(MerchantUser::getId, merchantUser.getId())
                .set(MerchantUser::getLoginIp, ip)
                .set(MerchantUser::getLoginDate, new Date()));

        // 封装 LoginUser
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(merchantUser.getId());
        loginUser.setUsername(merchantUser.getUsername());
        loginUser.setMerchantId(merchantUser.getMerchantId());
        loginUser.setStatus(UserConstants.NORMAL);
        return loginUser;
    }

    @Override
    public MerchantUser getByUsername(String username) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<MerchantUser>()
                        .eq(MerchantUser::getUsername, username)
                        .eq(MerchantUser::getDelFlag, "0"));
    }

    @Override
    public MerchantUser getByMerchantId(Long merchantId) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<MerchantUser>()
                        .eq(MerchantUser::getMerchantId, merchantId)
                        .eq(MerchantUser::getDelFlag, "0"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long id, String newPassword) {
        baseMapper.update(null, new LambdaUpdateWrapper<MerchantUser>()
                .eq(MerchantUser::getId, id)
                .set(MerchantUser::getPassword, SecurityUtils.encryptPassword(newPassword)));
    }
}
