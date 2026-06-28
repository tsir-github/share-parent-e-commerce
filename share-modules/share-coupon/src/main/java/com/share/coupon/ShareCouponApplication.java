package com.share.coupon;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 优惠券模块
 *
 * @author spzx
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class ShareCouponApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ShareCouponApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  优惠券模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
