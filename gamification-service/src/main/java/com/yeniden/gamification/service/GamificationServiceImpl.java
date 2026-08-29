package com.yeniden.gamification.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.gamification.domain.Badge;
import com.yeniden.gamification.domain.UserBadge;
import com.yeniden.gamification.domain.UserLevel;
import com.yeniden.gamification.dto.BadgeDto;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;
import com.yeniden.gamification.repository.BadgeRepository;
import com.yeniden.gamification.repository.UserBadgeRepository;
import com.yeniden.gamification.repository.UserLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gamification Service İş Mantığı Gerçekleşimi.
 */
@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserLevelRepository userLevelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BadgeDto> getAllBadges() {
        return badgeRepository.findAll()
                .stream()
                .map(this::mapToBadgeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeDto> getUserBadges(UUID userId) {
        return userBadgeRepository.findByUserId(userId)
                .stream()
                .map(userBadge -> {
                    Optional<Badge> badgeOpt = badgeRepository.findByCode(userBadge.getBadgeCode());
                    String name = badgeOpt.map(Badge::getName).orElse(userBadge.getBadgeCode());
                    String desc = badgeOpt.map(Badge::getDescription).orElse("");
                    String icon = badgeOpt.map(Badge::getIconKey).orElse("default_badge");

                    return UserBadgeDto.builder()
                            .id(userBadge.getId())
                            .userId(userBadge.getUserId())
                            .badgeCode(userBadge.getBadgeCode())
                            .badgeName(name)
                            .description(desc)
                            .iconKey(icon)
                            .earnedAt(userBadge.getEarnedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserLevelDto getUserLevel(UUID userId) {
        UserLevel userLevel = userLevelRepository.findById(userId)
                .orElseGet(() -> UserLevel.builder()
                        .userId(userId)
                        .level(1)
                        .totalPointsEarned(0)
                        .build());

        return mapToUserLevelDto(userLevel);
    }

    @Override
    @Transactional
    public UserBadgeDto awardBadge(UUID userId, String badgeCode) {
        Optional<UserBadge> existing = userBadgeRepository.findByUserIdAndBadgeCode(userId, badgeCode);
        if (existing.isPresent()) {
            UserBadge ub = existing.get();
            return UserBadgeDto.builder()
                    .id(ub.getId())
                    .userId(ub.getUserId())
                    .badgeCode(ub.getBadgeCode())
                    .earnedAt(ub.getEarnedAt())
                    .build();
        }

        UserBadge userBadge = UserBadge.builder()
                .userId(userId)
                .badgeCode(badgeCode)
                .build();

        UserBadge saved = userBadgeRepository.save(userBadge);

        Optional<Badge> badgeOpt = badgeRepository.findByCode(badgeCode);
        return UserBadgeDto.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .badgeCode(saved.getBadgeCode())
                .badgeName(badgeOpt.map(Badge::getName).orElse(badgeCode))
                .description(badgeOpt.map(Badge::getDescription).orElse(""))
                .iconKey(badgeOpt.map(Badge::getIconKey).orElse("default_badge"))
                .earnedAt(saved.getEarnedAt())
                .build();
    }

    @Override
    @Transactional
    public UserLevelDto addPoints(UUID userId, int points) {
        UserLevel userLevel = userLevelRepository.findById(userId)
                .orElseGet(() -> UserLevel.builder()
                        .userId(userId)
                        .level(1)
                        .totalPointsEarned(0)
                        .build());

        int newTotal = userLevel.getTotalPointsEarned() + points;
        int newLevel = (newTotal / 100) + 1;

        userLevel.setTotalPointsEarned(newTotal);
        userLevel.setLevel(newLevel);

        UserLevel saved = userLevelRepository.save(userLevel);
        return mapToUserLevelDto(saved);
    }

    private BadgeDto mapToBadgeDto(Badge badge) {
        return BadgeDto.builder()
                .id(badge.getId())
                .code(badge.getCode())
                .name(badge.getName())
                .description(badge.getDescription())
                .iconKey(badge.getIconKey())
                .build();
    }

    private UserLevelDto mapToUserLevelDto(UserLevel userLevel) {
        int pointsToNext = 100 - (userLevel.getTotalPointsEarned() % 100);
        return UserLevelDto.builder()
                .userId(userLevel.getUserId())
                .level(userLevel.getLevel())
                .totalPointsEarned(userLevel.getTotalPointsEarned())
                .pointsToNextLevel(pointsToNext)
                .build();
    }
}
