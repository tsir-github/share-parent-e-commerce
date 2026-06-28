package com.share.auth.service;

import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.system.api.model.LoginUser;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class H5LoginService {

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private SysRecordLogService recordLogService;
    //登录的方法
    public LoginUser login(String code) {
        //1 判断code是否为空
        if(StringUtils.isEmpty(code)) {
            throw new ServiceException("登录凭证不能为空");
        }
        //2 拿着code进行远程调用完成登录，返回userInfo
        R<UserInfo> userInfoR = remoteUserService.wxLogin(code);
        // 检查远程调用结果
        if (userInfoR == null || userInfoR.getCode() != 200) {
            throw new ServiceException("微信登录失败: " + (userInfoR != null ? userInfoR.getMsg() : "未知错误"));
        }
        UserInfo userInfo = userInfoR.getData();

        //3 判断返回userInfo是否为空
        if(userInfo == null) {
            throw new ServiceException("未获取到用户信息");
        }

        String status = userInfo.getStatus();
        if("1".equals(status)) {
            throw new ServiceException("账号被禁用");
        }

        //4 封装数据到LoginUser对象里面
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(userInfo.getId());
        loginUser.setUsername(userInfo.getWxOpenId());
        loginUser.setStatus(userInfo.getStatus()+"");

        //5 返回数据
        return loginUser;
    }
}
