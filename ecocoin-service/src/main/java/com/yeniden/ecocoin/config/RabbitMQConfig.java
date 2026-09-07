package com.yeniden.ecocoin.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String HANDOVER_EXCHANGE = "yeniden.exchange";
    public static final String HANDOVER_ROUTING_KEY = "handover.confirmed";
    public static final String HANDOVER_QUEUE = "ecocoin.handover.queue";
    public static final String DEAD_LETTER_EXCHANGE = "ecocoin.dlx";
    public static final String DEAD_LETTER_QUEUE = "ecocoin.handover.dlq";
    public static final String COINS_GRANTED_EXCHANGE = "yeniden.ecocoin";
    public static final String COINS_GRANTED_ROUTING_KEY = "coins.granted";
    public static final String GAMIFICATION_EXCHANGE = "yeniden.gamification";
    public static final String QUEST_COMPLETED_ROUTING_KEY = "quest.completed";
    public static final String QUEST_COMPLETED_QUEUE = "ecocoin.quest-completed.queue";
    public static final String QUEST_COMPLETED_DEAD_LETTER_QUEUE = "ecocoin.quest-completed.dlq";

    @Bean
    public TopicExchange handoverExchange() {
        return new TopicExchange(HANDOVER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue handoverQueue() {
        return new Queue(HANDOVER_QUEUE, true, false, false,
                java.util.Map.of("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE));
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding handoverBinding(Queue handoverQueue, TopicExchange handoverExchange) {
        return BindingBuilder.bind(handoverQueue).to(handoverExchange).with(HANDOVER_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(HANDOVER_QUEUE);
    }

    @Bean
    public TopicExchange coinsGrantedExchange() {
        return new TopicExchange(COINS_GRANTED_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange gamificationExchange() {
        return new TopicExchange(GAMIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue questCompletedQueue() {
        return new Queue(QUEST_COMPLETED_QUEUE, true, false, false,
                java.util.Map.of("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE));
    }

    @Bean
    public Queue questCompletedDeadLetterQueue() {
        return new Queue(QUEST_COMPLETED_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding questCompletedBinding(Queue questCompletedQueue, TopicExchange gamificationExchange) {
        return BindingBuilder.bind(questCompletedQueue).to(gamificationExchange).with(QUEST_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding questCompletedDeadLetterBinding(Queue questCompletedDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(questCompletedDeadLetterQueue).to(deadLetterExchange).with(QUEST_COMPLETED_QUEUE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory handoverRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter, RabbitTemplate rabbitTemplate) {
        return listenerFactory(connectionFactory, jsonMessageConverter, rabbitTemplate, HANDOVER_QUEUE);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory questRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter, RabbitTemplate rabbitTemplate) {
        return listenerFactory(connectionFactory, jsonMessageConverter, rabbitTemplate, QUEST_COMPLETED_QUEUE);
    }

    private SimpleRabbitListenerContainerFactory listenerFactory(ConnectionFactory connectionFactory,
                                                                   MessageConverter jsonMessageConverter,
                                                                   RabbitTemplate rabbitTemplate,
                                                                   String deadLetterRoutingKey) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, DEAD_LETTER_EXCHANGE, deadLetterRoutingKey))
                .build());
        return factory;
    }
}
