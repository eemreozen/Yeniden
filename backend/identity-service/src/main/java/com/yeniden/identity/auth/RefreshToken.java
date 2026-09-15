package com.yeniden.identity.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Consumed rows are retained as replay history; rotation never deletes or overwrites them. */
@Entity
@Table(name = "refresh_tokens", schema = "identity")
public class RefreshToken {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected RefreshToken() {}

    RefreshToken(String tokenHash, UUID sessionId, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash() { return tokenHash; }
    public UUID getSessionId() { return sessionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    void consume(Instant now) { consumedAt = now; }
}
