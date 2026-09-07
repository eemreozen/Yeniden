package com.yeniden.ecocoin.listener;

import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.ecocoin.service.EcoCoinService;
import com.yeniden.ecocoin.dto.CoinGrantResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandoverListener {

    private final EcoCoinService ecoCoinService;

    @RabbitListener(queues = "ecocoin.handover.queue", containerFactory = "handoverRabbitListenerContainerFactory")
    public void handleHandoverConfirmed(HandoverConfirmedEvent event) {
        // Propagate failures so the configured retry interceptor can retry and then DLQ the message.
        CoinGrantResult result = ecoCoinService.processHandover(event);
        if (result.isGranted()) {
            log.info("EcoCoin grant completed. handoverId={}, userId={}, amount={}",
                    event.getHandoverId(), event.getProviderId(), result.getAmount());
        } else {
            log.info("EcoCoin grant rejected. handoverId={}, userId={}, reason={}",
                    event.getHandoverId(), event.getProviderId(), result.getReason());
        }
    }
}
