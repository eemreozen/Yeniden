package com.yeniden.exchange.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.exchange.config.RabbitMQConfig;
import com.yeniden.exchange.domain.HandoverOutboxEvent;
import com.yeniden.exchange.repository.HandoverOutboxEventRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class HandoverOutboxPublisherTest {
    @Mock HandoverOutboxEventRepository outboxRepository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ObjectMapper objectMapper;

    @Test
    void marksSuccessfullyPublishedEvent() throws Exception {
        HandoverOutboxEvent row = row();
        HandoverConfirmedEvent payload = HandoverConfirmedEvent.builder().handoverId(row.getEventId()).build();
        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(row));
        when(objectMapper.readValue(row.getPayload(), HandoverConfirmedEvent.class)).thenReturn(payload);

        new HandoverOutboxPublisher(outboxRepository, rabbitTemplate, objectMapper).publishPendingEvents();

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, payload);
        assertNotNull(row.getPublishedAt());
    }

    @Test
    void leavesFailedEventPendingForRetry() throws Exception {
        HandoverOutboxEvent row = row();
        HandoverConfirmedEvent payload = HandoverConfirmedEvent.builder().handoverId(row.getEventId()).build();
        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(row));
        when(objectMapper.readValue(row.getPayload(), HandoverConfirmedEvent.class)).thenReturn(payload);
        doThrow(new IllegalStateException("broker unavailable")).when(rabbitTemplate)
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), any(HandoverConfirmedEvent.class));

        new HandoverOutboxPublisher(outboxRepository, rabbitTemplate, objectMapper).publishPendingEvents();

        assertNull(row.getPublishedAt());
    }

    private HandoverOutboxEvent row() {
        return HandoverOutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .eventType("HandoverConfirmedEvent")
                .aggregateId(UUID.randomUUID())
                .payload("{}")
                .build();
    }
}
