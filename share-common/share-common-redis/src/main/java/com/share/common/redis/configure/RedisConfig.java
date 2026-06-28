package com.share.common.redis.configure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import com.share.common.redis.service.IdempotentService;
import com.share.common.redis.service.RedisService;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置
 *
 * @author share
 */
@Configuration
@EnableCaching
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class RedisConfig extends CachingConfigurerSupport
{

    /**
     * 显式声明 RedisSentinelConfiguration Bean，修复 Nacos @ConfigurationProperties
     * 绑定嵌套 sentinel 属性不生效的问题（@Value 正常但 RedisProperties.getSentinel() 返回 null）。
     * <p>
     * 该 Bean 会被 {@code LettuceConnectionConfiguration} 的
     * {@code ObjectProvider<RedisSentinelConfiguration>} 参数优先获取，
     * 从而使用哨兵模式而非回退到 standalone localhost:6379。
     */
    @Bean
    @ConditionalOnProperty("spring.redis.sentinel.master")
    public RedisSentinelConfiguration redisSentinelConfiguration(
            @Value("${spring.redis.sentinel.master}") String master,
            @Value("${spring.redis.sentinel.nodes}") String nodes,
            @Value("${spring.redis.password:}") String password,
            @Value("${spring.redis.database:0}") int database) {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(master);
        for (String node : nodes.split(",")) {
            config.addSentinel(RedisNode.fromString(node.trim()));
        }
        config.setDatabase(database);
        if (!password.isEmpty()) {
            config.setPassword(RedisPassword.of(password));
        }
        return config;
    }

    @Bean
    @SuppressWarnings(value = { "unchecked", "rawtypes" })
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory)
    {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisService redisService() {
        return new RedisService();
    }

    @Bean
    public IdempotentService idempotentService(RedisService redisService) {
        return new IdempotentService(redisService);
    }
}
