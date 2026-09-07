package com.yeniden.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Teslimat kodu doğrulandığında RabbitMQ üzerinden yayınlanan asenkron olay (Event-Driven Architecture).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverConfirmedEvent implements Serializable {
    private UUID handoverId;
    private UUID requestId;
    private UUID listingId;
    private UUID providerId;
    private UUID receiverId;
    private UUID categoryId;
    private BigDecimal categoryCoinMultiplier;
    private String quantityBand;
    private boolean reviewRequired;
    private String handoverStatus;
    private LocalDateTime confirmedAt;
}
