package com.yeniden.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinsGrantedEvent implements Serializable {
    private UUID eventId;
    private UUID userId;
    private int amount;
    private String reason;
    private UUID sourceHandoverId;
}
