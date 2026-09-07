package com.yeniden.ecocoin.config;

import com.yeniden.ecocoin.listener.HandoverListener;
import com.yeniden.ecocoin.listener.QuestCompletedListener;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitMQConfigTest {
    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void usesSeparateDeadLetterQueuesForEachInboundEventType() {
        assertEquals(RabbitMQConfig.DEAD_LETTER_QUEUE, config.deadLetterQueue().getName());
        assertEquals(RabbitMQConfig.QUEST_COMPLETED_DEAD_LETTER_QUEUE, config.questCompletedDeadLetterQueue().getName());
        assertEquals(RabbitMQConfig.HANDOVER_QUEUE,
                config.handoverQueue().getArguments().get("x-dead-letter-routing-key"));
        assertEquals(RabbitMQConfig.QUEST_COMPLETED_QUEUE,
                config.questCompletedQueue().getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void listenersUseTheirOwnRetryContainers() throws NoSuchMethodException {
        RabbitListener handover = HandoverListener.class
                .getMethod("handleHandoverConfirmed", com.yeniden.common.event.HandoverConfirmedEvent.class)
                .getAnnotation(RabbitListener.class);
        RabbitListener quest = QuestCompletedListener.class
                .getMethod("handle", com.yeniden.common.event.QuestCompletedEvent.class)
                .getAnnotation(RabbitListener.class);

        assertEquals("handoverRabbitListenerContainerFactory", handover.containerFactory());
        assertEquals("questRabbitListenerContainerFactory", quest.containerFactory());
    }
}
