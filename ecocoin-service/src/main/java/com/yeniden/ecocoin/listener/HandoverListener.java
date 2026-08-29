package com.yeniden.ecocoin.listener;

import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.service.EcoCoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandoverListener {

    private final EcoCoinService ecoCoinService;

    /**
     * exchange-service teslimat doğrulandığında RabbitMQ kuyruğuna olay yollar.
     * ecocoin-service bu olayı asenkron yakalar ve vergi/limit kurallarına uyarak cüzdana EcoCoin tanımlar.
     */
    @RabbitListener(queues = "ecocoin.handover.queue")
    public void handleHandoverConfirmed(HandoverConfirmedEvent event) {
        log.info("RabbitMQ Event Alındı -> HandoverId: {}, ProviderId: {}, Points: {}",
                event.getHandoverId(), event.getProviderId(), event.getEarnedPoints());

        // Veren kişiye (provider) ödül puanlarını tanımla
        GrantCoinsRequest request = GrantCoinsRequest.builder()
                .userId(event.getProviderId())
                .amount(event.getEarnedPoints())
                .idempotencyKey("HANDOVER:" + event.getHandoverId())
                .description("Teslimat Tamamlama Ödülü (İlan ID: " + event.getListingId() + ")")
                .build();

        try {
            ecoCoinService.grantCoins(request);
            log.info("EcoCoin başarıyla hesaba aktarıldı! UserId: {}", event.getProviderId());
        } catch (Exception e) {
            log.error("EcoCoin aktarım hatası: {}", e.getMessage());
        }
    }
}
