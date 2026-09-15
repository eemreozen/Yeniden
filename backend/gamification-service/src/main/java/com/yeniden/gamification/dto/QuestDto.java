package com.yeniden.gamification.dto;
import lombok.Builder; import lombok.Getter;
import java.util.UUID;
@Getter @Builder public class QuestDto { private final UUID id; private final String period; private final String code; private final String title; private final String targetMetric; private final long targetValue; private final long rewardCoins; }
