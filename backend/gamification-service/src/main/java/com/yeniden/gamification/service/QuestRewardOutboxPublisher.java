package com.yeniden.gamification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.QuestCompletedEvent;
import com.yeniden.gamification.config.RabbitMQConfig;
import com.yeniden.gamification.domain.QuestRewardOutboxEvent;
import com.yeniden.gamification.repository.QuestRewardOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestRewardOutboxPublisher {
    private final QuestRewardOutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${gamification.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        for (QuestRewardOutboxEvent event : outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                QuestCompletedEvent payload = objectMapper.readValue(event.getPayload(), QuestCompletedEvent.class);
                rabbitTemplate.convertAndSend(RabbitMQConfig.GAMIFICATION_EXCHANGE,
                        RabbitMQConfig.QUEST_COMPLETED_ROUTING_KEY, payload);
                event.setPublishedAt(java.time.LocalDateTime.now());
            } catch (Exception exception) {
                log.error("Quest reward outbox publish failed. eventId={}", event.getEventId(), exception);
            }
        }
    }
}
