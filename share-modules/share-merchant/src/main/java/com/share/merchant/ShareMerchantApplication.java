package com.share.merchant;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商家模块启动类
 *
 * @author share
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class ShareMerchantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareMerchantApplication.class, args);
    }
}
