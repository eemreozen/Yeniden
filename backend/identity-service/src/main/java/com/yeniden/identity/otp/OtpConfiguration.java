package com.yeniden.identity.otp;

import com.yeniden.identity.sms.SmsSender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtpProperties.class)
public class OtpConfiguration {
    @Bean
    OtpService otpService(OtpProperties properties, Environment environment,
                          StringRedisTemplate redis, SmsSender sender) {
        if (properties.getNamespace() == null && environment.acceptsProfiles(Profiles.of("local"))) {
            properties.setNamespace("yeniden:identity:otp:local");
        }
        properties.validate();
        return new OtpService(new RedisOtpStore(redis, properties), sender, new OtpCrypto(properties));
    }
}
