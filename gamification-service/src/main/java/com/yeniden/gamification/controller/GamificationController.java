package com.yeniden.gamification.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.gamification.dto.BadgeDto;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;
import com.yeniden.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gamification Service REST API Uç Noktaları.
 */
@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Gamification Service çalışıyor!"));
    }

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<BadgeDto>>> getAllBadges() {
        List<BadgeDto> badges = gamificationService.getAllBadges();
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    @GetMapping("/users/{userId}/badges")
    public ResponseEntity<ApiResponse<List<UserBadgeDto>>> getUserBadges(@PathVariable("userId") UUID userId) {
        List<UserBadgeDto> badges = gamificationService.getUserBadges(userId);
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    @GetMapping("/users/{userId}/level")
    public ResponseEntity<ApiResponse<UserLevelDto>> getUserLevel(@PathVariable("userId") UUID userId) {
        UserLevelDto level = gamificationService.getUserLevel(userId);
        return ResponseEntity.ok(ApiResponse.success(level));
    }

    @PostMapping("/users/{userId}/badges/award")
    public ResponseEntity<ApiResponse<UserBadgeDto>> awardBadge(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "code") String badgeCode) {

        UserBadgeDto dto = gamificationService.awardBadge(userId, badgeCode);
        return ResponseEntity.ok(ApiResponse.success("Rozet başarıyla tanımlandı", dto));
    }

    @PostMapping("/users/{userId}/points/add")
    public ResponseEntity<ApiResponse<UserLevelDto>> addPoints(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "points") int points) {

        UserLevelDto dto = gamificationService.addPoints(userId, points);
        return ResponseEntity.ok(ApiResponse.success("Deneyim puanı eklendi", dto));
    }
}
