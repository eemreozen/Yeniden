package com.yeniden.gamification.listener;

import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class GamificationEventListener {
    private final GamificationService gamificationService;
    @RabbitListener(queues = "gamification.coins-granted.queue") public void coinsGranted(CoinsGrantedEvent event) { gamificationService.handleCoinsGranted(event); }
    @RabbitListener(queues = "gamification.handover-confirmed.queue") public void handoverConfirmed(HandoverConfirmedEvent event) { gamificationService.handleHandoverConfirmed(event); }
}
