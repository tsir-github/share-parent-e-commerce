package com.share.payment;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 支付模块
 *
 * @author spzx
 */
@EnableCustomConfig
@EnableRyFeignClients
@EnableScheduling
@SpringBootApplication
public class SharePaymentApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SharePaymentApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  支付模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
