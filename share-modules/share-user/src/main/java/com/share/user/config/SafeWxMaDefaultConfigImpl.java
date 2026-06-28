package com.share.user.config;

import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;

/**
 * 绕过 Gson 序列化导致的 JDK 模块系统限制
 * WxMaDefaultConfigImpl.toString() 内部用 Gson 序列化所有字段，
 * JDK 21 模块系统禁止反射访问 java.base 内部字段。
 * 覆盖 toString() 避免该问题。
 */
public class SafeWxMaDefaultConfigImpl extends WxMaDefaultConfigImpl {

    @Override
    public String toString() {
        return "WxMaConfig{appid='" + getAppid() + "'}";
    }
}