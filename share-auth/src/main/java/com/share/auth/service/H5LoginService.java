package com.share.auth.service;

import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.system.api.model.LoginUser;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.dto.WxLoginResultDTO;
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
        if(StringUtils.isEmpty(code)) throw new ServiceException("登录凭证不能为空");

        R<WxLoginResultDTO> result = remoteUserService.wxLogin(code);
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new ServiceException("微信登录失败: " + (result != null ? result.getMsg() : "未知错误"));
        }
        WxLoginResultDTO dto = result.getData();
        UserInfo userInfo = dto.getUserInfo();
        if(userInfo == null) throw new ServiceException("未获取到用户信息");
        if("1".equals(userInfo.getStatus())) throw new ServiceException("账号被禁用");

        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(userInfo.getId());
        loginUser.setUsername(userInfo.getWxOpenId());
        loginUser.setStatus(userInfo.getStatus()+"");
        return loginUser;
    }
}
