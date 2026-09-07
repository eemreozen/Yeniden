package com.yeniden.gamification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class RabbitMQConfig {
    public static final String GAMIFICATION_EXCHANGE = "yeniden.gamification";
    public static final String QUEST_COMPLETED_ROUTING_KEY = "quest.completed";
    @Bean TopicExchange gamificationExchange() { return new TopicExchange(GAMIFICATION_EXCHANGE, true, false); }
    @Bean TopicExchange ecoCoinExchange() { return new TopicExchange("yeniden.ecocoin", true, false); }
    @Bean TopicExchange handoverExchange() { return new TopicExchange("yeniden.exchange", true, false); }
    @Bean Queue coinsGrantedQueue() { return new Queue("gamification.coins-granted.queue", true); }
    @Bean Queue handoverConfirmedQueue() { return new Queue("gamification.handover-confirmed.queue", true); }
    @Bean Binding coinsGrantedBinding() { return BindingBuilder.bind(coinsGrantedQueue()).to(ecoCoinExchange()).with("coins.granted"); }
    @Bean Binding handoverConfirmedBinding() { return BindingBuilder.bind(handoverConfirmedQueue()).to(handoverExchange()).with("handover.confirmed"); }
    @Bean MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
