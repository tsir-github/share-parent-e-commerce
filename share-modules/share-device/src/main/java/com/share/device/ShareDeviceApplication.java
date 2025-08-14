package com.share.device;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

/**
 * 设备模块
 *
 * @author share
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
//@ComponentScan(basePackages = {"com.share.rules"})
@EnableFeignClients(basePackages = {
        "com.share.system.api",  // 添加系统API包扫描
        "com.share.device.feign", // 原有设备Feign扫描
        "com.share.rule.api"// 添加规则API包扫描
})
public class ShareDeviceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ShareDeviceApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  设备模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}