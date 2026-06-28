package com.share.user.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WxProperties.class)
public class WxMaConfig {

    private final WxProperties wxMaProperties;

    public WxMaConfig(WxProperties wxMaProperties) {
        this.wxMaProperties = wxMaProperties;
    }

    @Bean
    public WxMaService wxMaService() {
        SafeWxMaDefaultConfigImpl config = new SafeWxMaDefaultConfigImpl();
        config.setAppid(wxMaProperties.getAppId());
        config.setSecret(wxMaProperties.getSecret());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
