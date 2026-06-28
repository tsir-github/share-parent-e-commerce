package com.share.order.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 手动配置
 *
 * 在 Spring Boot 3.x 下, rocketmq-spring-boot-starter 2.2.3 的自动配置条件评估可能不生效,
 * 导致 RocketMQTemplate bean 未创建。这里手动创建所需 bean 确保兼容。
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    @Value("${rocketmq.producer.group:}")
    private String producerGroup;

    @Bean(name = "defaultMQProducer", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    public DefaultMQProducer defaultMQProducer() {
        log.info("手动创建 DefaultMQProducer: nameServer={}, group={}", nameServer, producerGroup);
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);
        producer.setRetryTimesWhenSendAsyncFailed(2);
        producer.setMaxMessageSize(1024 * 1024 * 4);
        producer.setCompressMsgBodyOverHowmuch(1024 * 4);
        return producer;
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean(RocketMQTemplate.class)
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer defaultMQProducer,
                                             ObjectProvider<RocketMQMessageConverter> converterProvider) {
        log.info("手动创建 RocketMQTemplate");
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(defaultMQProducer);
        converterProvider.ifAvailable(converter ->
                template.setMessageConverter(converter.getMessageConverter()));
        return template;
    }
}
