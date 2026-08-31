package com.yeniden.identity.dto;

import java.util.UUID;

public record PublicUserDto(UUID id, String displayName, String avatarKey, int trustScore) {}

