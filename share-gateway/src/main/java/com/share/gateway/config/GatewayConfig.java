package com.share.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import com.share.gateway.handler.SentinelFallbackHandler;

/**
 * 网关限流配置
 *
 * <p>sentinelGatewayFilter 由 SentinelSCGAutoConfiguration 自动配置，
 * 此处不再手动声明，避免 Bean 名冲突。</p>
 *
 * @author share
 */
@Configuration
public class GatewayConfig
{
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelFallbackHandler sentinelGatewayExceptionHandler()
    {
        return new SentinelFallbackHandler();
    }
}
