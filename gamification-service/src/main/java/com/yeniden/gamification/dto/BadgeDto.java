package com.yeniden.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Rozet detaylarını sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String iconKey;
}
