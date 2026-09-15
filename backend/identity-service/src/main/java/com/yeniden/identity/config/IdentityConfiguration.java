package com.yeniden.identity.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class IdentityConfiguration {
    @Bean @Primary
    Clock identityClock() { return Clock.systemUTC(); }
}
