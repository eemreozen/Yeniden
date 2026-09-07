package com.yeniden.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestCompletedEvent implements Serializable {
    private UUID eventId;
    private UUID userId;
    private UUID questId;
    private long rewardCoins;
    private Instant completedAt;
}
