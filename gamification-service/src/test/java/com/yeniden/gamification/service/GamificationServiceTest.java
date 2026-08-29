package com.yeniden.gamification.service;

import com.yeniden.gamification.domain.Badge;
import com.yeniden.gamification.domain.UserBadge;
import com.yeniden.gamification.domain.UserLevel;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;
import com.yeniden.gamification.repository.BadgeRepository;
import com.yeniden.gamification.repository.UserBadgeRepository;
import com.yeniden.gamification.repository.UserLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private UserLevelRepository levelRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private GamificationServiceImpl gamificationService;

    private UUID userId;
    private UserLevel userLevel;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userLevel = UserLevel.builder()
                .userId(userId)
                .level(1)
                .totalPointsEarned(50)
                .build();
    }

    @Test
    @DisplayName("Puan ekleme ve seviye yükseltme (Level 3) senaryosu")
    void addPoints_LevelUp_Success() {
        when(levelRepository.findById(userId)).thenReturn(Optional.of(userLevel));
        when(levelRepository.save(any(UserLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 50 mevcut + 150 yeni puan = 200 puan -> Level 3 olmalı (200 / 100 + 1 = 3)
        UserLevelDto result = gamificationService.addPoints(userId, 150);

        assertNotNull(result);
        assertEquals(200, result.getTotalPointsEarned());
        assertEquals(3, result.getLevel());

        verify(levelRepository).save(any(UserLevel.class));
    }

    @Test
    @DisplayName("Rozet kazanma senaryosu")
    void awardBadge_Success() {
        Badge badge = Badge.builder()
                .id(UUID.randomUUID())
                .code("FIRST_SHARE")
                .name("İlk İlan Paylaşımı")
                .description("İlk geri dönüşüm ilanını verdin!")
                .iconKey("badge_share_1")
                .build();

        UserBadge mockUserBadge = UserBadge.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .badgeCode("FIRST_SHARE")
                .build();

        when(userBadgeRepository.findByUserIdAndBadgeCode(userId, "FIRST_SHARE")).thenReturn(Optional.empty());
        when(userBadgeRepository.save(any(UserBadge.class))).thenReturn(mockUserBadge);
        when(badgeRepository.findByCode("FIRST_SHARE")).thenReturn(Optional.of(badge));

        UserBadgeDto result = gamificationService.awardBadge(userId, "FIRST_SHARE");

        assertNotNull(result);
        assertEquals("FIRST_SHARE", result.getBadgeCode());
        assertEquals("İlk İlan Paylaşımı", result.getBadgeName());

        verify(userBadgeRepository).save(any(UserBadge.class));
    }
}
