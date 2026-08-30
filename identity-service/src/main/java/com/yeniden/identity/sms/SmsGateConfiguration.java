package com.yeniden.identity.sms;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SmsGateProperties.class)
public class SmsGateConfiguration {
    // Always supply the port: disabled mode must reject sends, never simulate success.
    @Bean(destroyMethod = "close")
    public SmsGateSender smsSender(SmsGateProperties properties) {
        return new SmsGateSender(properties);
    }
}
