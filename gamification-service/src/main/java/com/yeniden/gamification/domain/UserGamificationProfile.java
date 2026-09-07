package com.yeniden.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_gamification_profiles", schema = "gamification")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserGamificationProfile {
    @Id private UUID userId;
    @Builder.Default @Column(nullable = false) private long totalCoinsEarned = 0;
    @Builder.Default @Column(nullable = false) private int level = 1;
    @Builder.Default @Column(nullable = false) private int listingsPublished = 0;
    @Builder.Default @Column(nullable = false) private int confirmedGiveCount = 0;
    @Builder.Default @Column(nullable = false) private int confirmedTakeCount = 0;
    @Builder.Default @Column(nullable = false) private int distinctCategoryCount = 0;
}
