package com.share.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;

/**
 * 系统模块
 *
 * @author share
 */
@RefreshScope // 添加此注解即时生效技巧,添加@RefreshScope到需要动态刷新的Bean
@EnableCustomConfig
@EnableRyFeignClients
@ComponentScan(basePackages = {"com.share"})
@SpringBootApplication
public class ShareSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(com.share.system.ShareSystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
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
}
