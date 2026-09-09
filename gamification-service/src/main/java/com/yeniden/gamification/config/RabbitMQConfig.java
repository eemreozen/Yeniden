package com.yeniden.gamification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class RabbitMQConfig {
    public static final String GAMIFICATION_EXCHANGE = "yeniden.gamification";
    public static final String QUEST_COMPLETED_ROUTING_KEY = "quest.completed";
    public static final String CATALOG_EXCHANGE = "yeniden.catalog";
    public static final String LISTING_PUBLISHED_ROUTING_KEY = "listing.published";
    public static final String LISTING_PUBLISHED_QUEUE = "gamification.listing-published.queue";
    public static final String COINS_GRANTED_QUEUE = "gamification.coins-granted.queue";
    public static final String HANDOVER_CONFIRMED_QUEUE = "gamification.handover-confirmed.queue";
    @Bean TopicExchange gamificationExchange() { return new TopicExchange(GAMIFICATION_EXCHANGE, true, false); }
    @Bean TopicExchange ecoCoinExchange() { return new TopicExchange("yeniden.ecocoin", true, false); }
    @Bean TopicExchange handoverExchange() { return new TopicExchange("yeniden.exchange", true, false); }
    @Bean TopicExchange catalogExchange() { return new TopicExchange(CATALOG_EXCHANGE, true, false); }
    @Bean Queue coinsGrantedQueue() { return new Queue(COINS_GRANTED_QUEUE, true); }
    @Bean Queue handoverConfirmedQueue() { return new Queue(HANDOVER_CONFIRMED_QUEUE, true); }
    @Bean Queue listingPublishedQueue() { return new Queue(LISTING_PUBLISHED_QUEUE, true); }
    @Bean Binding coinsGrantedBinding() { return BindingBuilder.bind(coinsGrantedQueue()).to(ecoCoinExchange()).with("coins.granted"); }
    @Bean Binding handoverConfirmedBinding() { return BindingBuilder.bind(handoverConfirmedQueue()).to(handoverExchange()).with("handover.confirmed"); }
    @Bean Binding listingPublishedBinding() { return BindingBuilder.bind(listingPublishedQueue()).to(catalogExchange()).with(LISTING_PUBLISHED_ROUTING_KEY); }
    @Bean MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
