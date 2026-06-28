package com.share.common.redis.configure;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置
 * <p>
 * 与 spring-boot-starter-data-redis 的 RedisTemplate 共存，
 * RedissonClient 专用于分布式锁（看门狗自动续期）。
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>单节点模式：配置 spring.redis.host / port / password</li>
 *   <li>哨兵模式：配置 spring.redis.sentinel.master + spring.redis.sentinel.nodes</li>
 * </ul>
 *
 * @author share
 */
@Configuration
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.redis.sentinel.nodes:}")
    private String sentinelNodes;

    /**
     * 连接超时（毫秒）
     */
    @Value("${spring.redis.timeout:10000}")
    private int timeout;

    /**
     * RedissonClient Bean
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * @Autowired
     * private RedissonClient redissonClient;
     *
     * RLock lock = redissonClient.getLock("order:pay:" + orderId);
     * try {
     *     if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
     *         // 业务逻辑
     *     }
     * } finally {
     *     if (lock.isHeldByCurrentThread()) {
     *         lock.unlock();
     *     }
     * }
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient() {
        Config config = new Config();

        if (!sentinelMaster.isEmpty() && !sentinelNodes.isEmpty()) {
            // ======== 哨兵模式 ========
            String[] nodes = sentinelNodes.split(",");
            String[] addresses = new String[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                String node = nodes[i].trim();
                addresses[i] = node.startsWith(REDIS_PROTOCOL_PREFIX) || node.startsWith(REDISS_PROTOCOL_PREFIX)
                        ? node : REDIS_PROTOCOL_PREFIX + node;
            }
            config.useSentinelServers()
                    .setMasterName(sentinelMaster)
                    .addSentinelAddress(addresses)
                    .setDatabase(database)
                    .setPassword(password.isEmpty() ? null : password)
                    .setTimeout(timeout);
            log.info("Redisson 初始化 [哨兵模式] master={}, nodes={}", sentinelMaster, sentinelNodes);
        } else {
            // ======== 单节点模式 ========
            String address = REDIS_PROTOCOL_PREFIX + host + ":" + port;
            SingleServerConfig singleConfig = config.useSingleServer()
                    .setAddress(address)
                    .setDatabase(database)
                    .setTimeout(timeout);
            if (!password.isEmpty()) {
                singleConfig.setPassword(password);
            }
            log.info("Redisson 初始化 [单节点模式] {}:{} db={}", host, port, database);
        }

        return Redisson.create(config);
    }
}
