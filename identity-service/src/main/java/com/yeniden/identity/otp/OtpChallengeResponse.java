package com.yeniden.identity.otp;

import java.util.UUID;

public record OtpChallengeResponse(UUID challengeId, long expiresIn, long retryAfterSeconds) { }
