package com.yeniden.exchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.exchange.config.RabbitMQConfig;
import com.yeniden.exchange.domain.HandoverOutboxEvent;
import com.yeniden.exchange.repository.HandoverOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandoverOutboxPublisher {
    private final HandoverOutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${exchange.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        for (HandoverOutboxEvent event : outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                HandoverConfirmedEvent payload = objectMapper.readValue(event.getPayload(), HandoverConfirmedEvent.class);
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, payload);
                event.setPublishedAt(java.time.LocalDateTime.now());
            } catch (Exception exception) {
                log.error("Handover outbox event will be retried. eventId={}", event.getEventId(), exception);
            }
        }
    }
}
