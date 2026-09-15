package com.yeniden.catalog.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Catalog Service RabbitMQ yapılandırması (01-modules.md Domain Event Sözleşmesi: ListingPublished).
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "yeniden.catalog";
    public static final String LISTING_PUBLISHED_ROUTING_KEY = "listing.published";

    @Bean
    public TopicExchange catalogExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
