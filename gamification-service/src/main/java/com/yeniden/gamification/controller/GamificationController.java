package com.yeniden.gamification.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.gamification.dto.BadgeDto;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;
import com.yeniden.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.YearMonth;

@RestController @RequestMapping("/api/v1/gamification") @RequiredArgsConstructor
public class GamificationController {
    private final GamificationService gamificationService;
    @GetMapping("/health") public ResponseEntity<ApiResponse<String>> healthCheck() { return ResponseEntity.ok(ApiResponse.success("Gamification Service is running")); }
    @GetMapping("/badges") public ResponseEntity<ApiResponse<List<BadgeDto>>> badges() { return ResponseEntity.ok(ApiResponse.success(gamificationService.getAllBadges())); }
    @GetMapping("/users/{userId}/badges") public ResponseEntity<ApiResponse<List<UserBadgeDto>>> userBadges(@PathVariable UUID userId) { return ResponseEntity.ok(ApiResponse.success(gamificationService.getUserBadges(userId))); }
    @GetMapping("/users/{userId}/level") public ResponseEntity<ApiResponse<UserLevelDto>> level(@PathVariable UUID userId) { return ResponseEntity.ok(ApiResponse.success(gamificationService.getUserLevel(userId))); }
    @GetMapping("/quests") public ResponseEntity<ApiResponse<List<com.yeniden.gamification.dto.QuestDto>>> quests(@RequestParam(defaultValue = "") String period) { return ResponseEntity.ok(ApiResponse.success(gamificationService.getQuests(period.isBlank() ? YearMonth.now().toString() : period))); }
    @GetMapping("/leaderboard") public ResponseEntity<ApiResponse<List<com.yeniden.gamification.dto.LeaderboardDto>>> leaderboard(@RequestParam(defaultValue = "") String period) { return ResponseEntity.ok(ApiResponse.success(gamificationService.getLeaderboard(period.isBlank() ? YearMonth.now().toString() : period))); }
}
