package com.share.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxProperties {
    private String appId;
    private String secret;
}
