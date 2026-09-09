package com.yeniden.gamification.listener;

import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.common.event.ListingPublishedEvent;
import com.yeniden.gamification.config.RabbitMQConfig;
import com.yeniden.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class GamificationEventListener {
    private final GamificationService gamificationService;
    @RabbitListener(queues = RabbitMQConfig.COINS_GRANTED_QUEUE) public void coinsGranted(CoinsGrantedEvent event) { gamificationService.handleCoinsGranted(event); }
    @RabbitListener(queues = RabbitMQConfig.HANDOVER_CONFIRMED_QUEUE) public void handoverConfirmed(HandoverConfirmedEvent event) { gamificationService.handleHandoverConfirmed(event); }
    @RabbitListener(queues = RabbitMQConfig.LISTING_PUBLISHED_QUEUE) public void listingPublished(ListingPublishedEvent event) { gamificationService.handleListingPublished(event); }
}
