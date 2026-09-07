package com.yeniden.gamification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.QuestCompletedEvent;
import com.yeniden.gamification.config.RabbitMQConfig;
import com.yeniden.gamification.domain.QuestRewardOutboxEvent;
import com.yeniden.gamification.repository.QuestRewardOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestRewardOutboxPublisherTest {
    @Mock QuestRewardOutboxEventRepository outbox;
    @Mock RabbitTemplate rabbitTemplate;

    @Test
    void publishesPendingQuestCompletionAndMarksItPublishedOnlyAfterSuccess() throws Exception {
        QuestRewardOutboxEvent pending = pendingEvent();
        when(outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(pending));

        new QuestRewardOutboxPublisher(outbox, rabbitTemplate, mapper()).publishPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.GAMIFICATION_EXCHANGE),
                eq(RabbitMQConfig.QUEST_COMPLETED_ROUTING_KEY), any(QuestCompletedEvent.class));
        assertNotNull(pending.getPublishedAt());
    }

    @Test
    void leavesFailedPublicationPendingForRetry() throws Exception {
        QuestRewardOutboxEvent pending = pendingEvent();
        when(outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(pending));
        doThrow(new IllegalStateException("broker unavailable")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        new QuestRewardOutboxPublisher(outbox, rabbitTemplate, mapper()).publishPendingEvents();

        assertNull(pending.getPublishedAt());
    }

    private QuestRewardOutboxEvent pendingEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        QuestCompletedEvent event = QuestCompletedEvent.builder().eventId(eventId).userId(UUID.randomUUID())
                .questId(UUID.randomUUID()).rewardCoins(10).completedAt(Instant.now()).build();
        return QuestRewardOutboxEvent.builder().eventId(eventId).eventType("QuestCompletedEvent")
                .aggregateId(event.getQuestId()).payload(mapper().writeValueAsString(event)).build();
    }

    private ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
