package com.share.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付 v3 配置
 *
 * 从 Nacos 配置中心读取（wx.pay.v3 前缀）。
 * 支持运行时动态刷新（@RefreshScope）。
 *
 * mock-mode=true 时跳过微信支付，走模拟流程（开发阶段使用）。
 *
 * Nacos 配置示例：
 * <pre>
 * wx:
 *   pay:
 *     v3:
 *       mock-mode: true
 *       app-id: your-app-id
 *       mch-id: your-mch-id
 *       api-key: your-api-v3-key
 *       cert-serial: your-cert-serial-no
 *       private-key-path: /path/to/apiclient_key.pem
 *       notify-url: https://your-domain.com/prod-api/payment/api/v1/payment/callback
 * </pre>
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "wx.pay.v3")
public class WxPayConfig {

    /** 是否启用模拟支付模式（开发阶段=true，上线前改为false） */
    private boolean mockMode = true;

    /** 小程序 appId */
    private String appId;

    /** 商户号 */
    private String mchId;

    /** API v3 密钥（用于回调数据解密） */
    private String apiKey;

    /** 商户证书序列号 */
    private String certSerial;

    /** 商户私钥文件路径 */
    private String privateKeyPath;

    /** 支付回调通知地址 */
    private String notifyUrl;

    /** 微信支付 API 基础地址（可覆盖，默认为生产环境） */
    private String apiBase = "https://api.mch.weixin.qq.com";
}
