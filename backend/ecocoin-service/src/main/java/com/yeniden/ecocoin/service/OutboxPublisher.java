package com.yeniden.ecocoin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.ecocoin.config.RabbitMQConfig;
import com.yeniden.ecocoin.domain.OutboxEvent;
import com.yeniden.ecocoin.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${ecocoin.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent event : outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                CoinsGrantedEvent payload = objectMapper.readValue(event.getPayload(), CoinsGrantedEvent.class);
                rabbitTemplate.convertAndSend(RabbitMQConfig.COINS_GRANTED_EXCHANGE,
                        RabbitMQConfig.COINS_GRANTED_ROUTING_KEY, payload);
                event.setPublishedAt(LocalDateTime.now());
            } catch (Exception exception) {
                log.error("Outbox event will be retried. eventId={}", event.getId(), exception);
            }
        }
    }
}
