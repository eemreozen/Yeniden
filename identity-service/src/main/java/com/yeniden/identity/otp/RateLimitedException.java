package com.yeniden.identity.otp;

import com.yeniden.common.exception.BaseException;

public final class RateLimitedException extends BaseException {
    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super("Lütfen yeniden denemeden önce bekleyin.", "rate_limited", 429);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
    public long retryAfterSeconds() { return retryAfterSeconds; }
}
