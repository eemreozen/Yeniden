package com.yeniden.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bir ilan yayınlandığında RabbitMQ üzerinden paylaşılan katalog olayı.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingPublishedEvent implements Serializable {
    private UUID listingId;
    private UUID ownerId;
    private UUID categoryId;
    private LocalDateTime publishedAt;
}
