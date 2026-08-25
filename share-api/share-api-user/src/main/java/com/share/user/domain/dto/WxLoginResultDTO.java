package com.share.user.domain.dto;

import com.share.user.domain.UserInfo;

public class WxLoginResultDTO {
    private String token;
    private UserInfo userInfo;
    private boolean isNewUser;

    public WxLoginResultDTO() {}
    public WxLoginResultDTO(String token, UserInfo userInfo, boolean isNewUser) {
        this.token = token; this.userInfo = userInfo; this.isNewUser = isNewUser;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }
    public boolean isNewUser() { return isNewUser; }
    public void setNewUser(boolean isNewUser) { this.isNewUser = isNewUser; }
}
