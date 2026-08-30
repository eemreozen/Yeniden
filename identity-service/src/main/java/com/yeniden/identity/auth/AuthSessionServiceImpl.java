package com.yeniden.identity.auth;

import com.yeniden.identity.domain.User;
import com.yeniden.identity.domain.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthSessionServiceImpl implements AuthSessionService {
    private final AuthUserLockRepository users;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final RefreshTokenCodec codec;
    private final AccessTokenIssuer accessTokens;
    private final TokenProperties properties;
    private final Clock clock;
    private final TransactionTemplate issueTransaction;
    private final TransactionTemplate rotationTransaction;
    private final TransactionTemplate readTransaction;

    public AuthSessionServiceImpl(AuthUserLockRepository users, AuthSessionRepository sessions,
            RefreshTokenRepository refreshTokens, RefreshTokenCodec codec, AccessTokenIssuer accessTokens,
            TokenProperties properties, @Qualifier("authClock") Clock clock, PlatformTransactionManager transactionManager) {
        this.users = users;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.codec = codec;
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
        issueTransaction = new TransactionTemplate(transactionManager);
        // OTP user creation + session issuance remains atomic in the caller's transaction.
        rotationTransaction = new TransactionTemplate(transactionManager);
        rotationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        rotationTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        readTransaction = new TransactionTemplate(transactionManager);
        readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        readTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        readTransaction.setReadOnly(true);
    }

    @Override
    public TokenResponse issue(User user, boolean isNewUser) {
        if (user == null || user.getId() == null) throw AuthException.unauthorized();
        return Objects.requireNonNull(issueTransaction.execute(status -> {
            User locked = users.lockById(user.getId()).orElseThrow(AuthException::unauthorized);
            if (!eligible(locked)) throw AuthException.unauthorized();
            Instant now = now();
            AuthSession session = sessions.save(new AuthSession(UUID.randomUUID(), locked.getId(), now,
                    now.plus(properties.getRefreshTtl())));
            return mint(locked, session, now, isNewUser);
        }));
    }

    @Override
    public TokenResponse refresh(String raw) {
        String hash = codec.hash(raw);
        Rotation result = Objects.requireNonNull(rotationTransaction.execute(status -> rotate(hash)));
        // Deliberately outside REQUIRES_NEW: replay revocation has COMMITTED even if the caller rolls back.
        if (result.failure != null) throw result.failure;
        return result.tokens;
    }

    private Rotation rotate(String hash) {
        UUID userId = refreshTokens.findOwnerId(hash).orElse(null);
        if (userId == null) return Rotation.rejected(AuthException.unauthorized());
        User user = users.lockById(userId).orElse(null);
        if (user == null) return Rotation.rejected(AuthException.unauthorized());
        // Mutable token/session state is first loaded AFTER the shared user lock.
        RefreshToken token = refreshTokens.findById(hash).orElse(null);
        if (token == null) return Rotation.rejected(AuthException.unauthorized());
        Instant now = now();
        // Reuse is checked before expiry/revocation/account status, including old consumed tokens.
        if (token.getConsumedAt() != null) {
            sessions.revokeAll(userId, now);
            return Rotation.rejected(AuthException.refreshReused());
        }
        if (!eligible(user)) {
            sessions.revokeAll(userId, now);
            return Rotation.rejected(AuthException.unauthorized());
        }
        AuthSession session = sessions.findById(token.getSessionId()).orElse(null);
        if (session == null || !session.getUserId().equals(userId) || !session.activeAt(now)
                || !token.getExpiresAt().isAfter(now)) return Rotation.rejected(AuthException.unauthorized());
        token.consume(now);
        refreshTokens.save(token);
        return new Rotation(mint(user, session, now, false), null);
    }

    @Override
    public void logout(String raw) {
        final String hash;
        try { hash = codec.hash(raw); }
        catch (AuthException invalid) { return; } // Idempotent, non-enumerating logout.
        rotationTransaction.executeWithoutResult(status -> {
            UUID owner = refreshTokens.findOwnerId(hash).orElse(null);
            if (owner == null || users.lockById(owner).isEmpty()) return;
            refreshTokens.findById(hash).flatMap(token -> sessions.findById(token.getSessionId()))
                    .filter(session -> session.getUserId().equals(owner)).ifPresent(session -> {
                        session.revoke(now());
                        sessions.save(session);
                    });
        });
    }

    @Override
    public void assertActive(UUID userId, UUID sessionId) {
        if (userId == null || sessionId == null) throw AuthException.unauthorized();
        boolean active = Boolean.TRUE.equals(readTransaction.execute(status ->
                sessions.existsActive(userId, sessionId, clock.instant(), UserStatus.ACTIVE)));
        if (!active) throw AuthException.unauthorized();
    }

    private TokenResponse mint(User user, AuthSession session, Instant now, boolean newUser) {
        Instant accessExpiry = now.plus(properties.getAccessTtl());
        if (accessExpiry.isAfter(session.getExpiresAt())) accessExpiry = session.getExpiresAt();
        String raw = codec.generate();
        refreshTokens.save(new RefreshToken(codec.hash(raw), session.getId(), now, session.getExpiresAt()));
        String access = accessTokens.issue(user, session, now, accessExpiry);
        return new TokenResponse(access, raw, newUser, Duration.between(now, accessExpiry).toSeconds());
    }

    private boolean eligible(User user) {
        return user.getStatus() == UserStatus.ACTIVE && user.getPhoneVerifiedAt() != null;
    }

    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.SECONDS); }

    private record Rotation(TokenResponse tokens, AuthException failure) {
        private static Rotation rejected(AuthException failure) { return new Rotation(null, failure); }
    }
}
