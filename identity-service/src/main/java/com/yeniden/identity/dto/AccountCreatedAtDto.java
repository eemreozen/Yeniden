package com.yeniden.identity.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable account-age data consumed by downstream services when applying
 * account-age based business rules.
 */
public record AccountCreatedAtDto(UUID userId, Instant accountCreatedAt) {}
