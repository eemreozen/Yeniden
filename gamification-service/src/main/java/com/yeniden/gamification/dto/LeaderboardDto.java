package com.yeniden.gamification.dto;
import lombok.Builder; import lombok.Getter;
import java.util.UUID;
@Getter @Builder public class LeaderboardDto { private final UUID userId; private final int rank; private final long score; private final String scope; private final String period; }
