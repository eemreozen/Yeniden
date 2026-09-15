package com.yeniden.ecocoin.listener;

import com.yeniden.common.event.QuestCompletedEvent;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.CoinGrantResult;
import com.yeniden.ecocoin.service.EcoCoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestCompletedListener {
    private final EcoCoinService ecoCoinService;

    @RabbitListener(queues = "ecocoin.quest-completed.queue", containerFactory = "questRabbitListenerContainerFactory")
    public void handle(QuestCompletedEvent event) {
        if (event == null || event.getUserId() == null || event.getQuestId() == null || event.getEventId() == null || event.getRewardCoins() <= 0) {
            log.warn("Ignoring invalid QuestCompletedEvent");
            return;
        }
        CoinGrantResult result = ecoCoinService.grantCoins(GrantCoinsRequest.builder().userId(event.getUserId())
                .amount(event.getRewardCoins()).reason("QUEST_COMPLETED").sourceRef("quest:" + event.getQuestId())
                .idempotencyKey("quest:" + event.getUserId() + ":" + event.getQuestId()).build());
        if (result.isGranted()) log.info("Quest reward granted. questId={}, userId={}, amount={}", event.getQuestId(), event.getUserId(), result.getAmount());
        else log.info("Quest reward rejected. questId={}, userId={}, reason={}", event.getQuestId(), event.getUserId(), result.getReason());
    }
}
