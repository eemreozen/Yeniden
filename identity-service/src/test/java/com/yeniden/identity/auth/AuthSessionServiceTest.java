package com.yeniden.identity.auth;

import com.yeniden.identity.domain.User;
import com.yeniden.identity.domain.UserRole;
import com.yeniden.identity.domain.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthSessionServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
    private final AuthUserLockRepository users = mock(AuthUserLockRepository.class);
    private final AuthSessionRepository sessions = mock(AuthSessionRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final RefreshTokenCodec codec = new RefreshTokenCodec();
    private final TokenProperties properties = new TokenProperties();
    private AuthSessionServiceImpl service;
    private User user;
    private AuthSession session;
    private RefreshToken token;
    private String raw;

    @BeforeEach
    void setUp() {
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        service = new AuthSessionServiceImpl(users, sessions, refreshTokens, codec, issuer, properties,
                Clock.fixed(NOW, ZoneOffset.UTC), transactions);
        user = User.builder().id(UUID.randomUUID()).phoneVerifiedAt(NOW.minusSeconds(60))
                .status(UserStatus.ACTIVE).roles(Set.of(UserRole.USER)).build();
        session = new AuthSession(UUID.randomUUID(), user.getId(), NOW.minus(Duration.ofDays(5)), NOW.plus(Duration.ofDays(25)));
        raw = codec.generate();
        token = new RefreshToken(codec.hash(raw), session.getId(), session.getCreatedAt(), session.getExpiresAt());
    }

    private void knownToken() {
        when(refreshTokens.findOwnerId(token.getTokenHash())).thenReturn(Optional.of(user.getId()));
        when(users.lockById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokens.findById(token.getTokenHash())).thenReturn(Optional.of(token));
    }

    @Test
    void issuePersistsOnlyHashAndJoinsVerifiedUserTransaction() {
        when(users.lockById(user.getId())).thenReturn(Optional.of(user));
        when(sessions.save(any())).thenAnswer(call -> call.getArgument(0));
        when(issuer.issue(eq(user), any(), eq(NOW), eq(NOW.plusSeconds(900)))).thenReturn("signed");
        TokenResponse result = service.issue(user, true);
        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(codec.hash(result.refreshToken()));
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.expiresIn()).isEqualTo(900);
        verify(transactions).getTransaction(argThat(def -> def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED));
        verify(transactions).commit(any());
    }

    @Test
    void issueReloadsAccountRatherThanTrustingCallerSuppliedStatus() {
        User suspended = User.builder().id(user.getId()).status(UserStatus.SUSPENDED).phoneVerifiedAt(NOW).build();
        when(users.lockById(user.getId())).thenReturn(Optional.of(suspended));
        assertThatThrownBy(() -> service.issue(user, false)).isInstanceOf(AuthException.class);
        verifyNoInteractions(sessions, refreshTokens, issuer);
        verify(transactions).rollback(any());
    }

    @Test
    void unverifiedUserCannotReceiveSession() {
        user.setPhoneVerifiedAt(null);
        when(users.lockById(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.issue(user, false)).isInstanceOf(AuthException.class);
        verifyNoInteractions(sessions, refreshTokens, issuer);
    }

    @Test
    void rotationConsumesHistoryAndKeepsOriginalAbsoluteExpiry() {
        knownToken();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        when(issuer.issue(user, session, NOW, NOW.plusSeconds(900))).thenReturn("rotated-access");
        TokenResponse response = service.refresh(raw);
        assertThat(response.refreshToken()).isNotEqualTo(raw);
        assertThat(response.isNewUser()).isFalse();
        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens, times(2)).save(saved.capture());
        RefreshToken replacement = saved.getAllValues().get(1);
        assertThat(replacement.getExpiresAt()).isEqualTo(session.getExpiresAt());
        assertThat(replacement.getTokenHash()).isEqualTo(codec.hash(response.refreshToken()));
        var order = inOrder(users, refreshTokens);
        order.verify(refreshTokens).findOwnerId(token.getTokenHash());
        order.verify(users).lockById(user.getId());
        order.verify(refreshTokens).findById(token.getTokenHash());
        verify(transactions).getTransaction(argThat(def ->
                def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW
                        && def.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED));
        verify(transactions).commit(any());
    }

    @Test
    void reuseRevokesEverySessionAndCommitsBeforeThrowingEvenForExpiredHistory() {
        token = new RefreshToken(codec.hash(raw), session.getId(), NOW.minus(Duration.ofDays(40)), NOW.minusSeconds(1));
        token.consume(NOW.minus(Duration.ofDays(35)));
        knownToken();
        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOfSatisfying(AuthException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("refresh_token_reused"));
        var order = inOrder(sessions, transactions);
        order.verify(sessions).revokeAll(user.getId(), NOW);
        order.verify(transactions).commit(any(TransactionStatus.class));
        verify(transactions, never()).rollback(any());
        verifyNoInteractions(issuer);
    }

    @Test
    void invalidAccountRevokesAllAndCommits() {
        user.setStatus(UserStatus.DELETED);
        knownToken();
        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(AuthException.class);
        verify(sessions).revokeAll(user.getId(), NOW);
        verify(transactions).commit(any());
        verify(transactions, never()).rollback(any());
    }

    @Test
    void accessExpiryIsCappedByAbsoluteSessionExpiry() {
        session = new AuthSession(session.getId(), user.getId(), NOW.minus(Duration.ofDays(30)).plusSeconds(20), NOW.plusSeconds(20));
        knownToken();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        assertThat(service.refresh(raw).expiresIn()).isEqualTo(20);
        verify(issuer).issue(user, session, NOW, NOW.plusSeconds(20));
    }

    @Test
    void expiryBoundaryCannotBeRefreshed() {
        session = new AuthSession(session.getId(), user.getId(), NOW.minus(Duration.ofDays(30)), NOW);
        knownToken();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(AuthException.class);
        verifyNoInteractions(issuer);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void revokedSessionCannotBeRefreshed() {
        session.revoke(NOW.minusSeconds(1));
        knownToken();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(AuthException.class);
        verifyNoInteractions(issuer);
    }

    @Test
    void encoderFailureRollsBackRotation() {
        knownToken();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        when(issuer.issue(any(), any(), any(), any())).thenThrow(new IllegalStateException("signer unavailable"));
        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(IllegalStateException.class);
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
    }

    @Test
    void logoutAcceptsConsumedTokenAndOnlyRevokesItsSession() {
        token.consume(NOW.minusSeconds(1));
        knownToken();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        service.logout(raw);
        service.logout(raw);
        assertThat(session.getRevokedAt()).isEqualTo(NOW);
        verify(sessions, never()).revokeAll(any(), any());
        verify(transactions, times(2)).commit(any());
    }

    @Test
    void unknownLogoutIsIdempotentAndUnknownRefreshIsRejected() {
        service.logout("not-a-token");
        service.logout(raw);
        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(AuthException.class);
        verifyNoInteractions(users, sessions, issuer);
    }

    @Test
    void activeCheckRequiresMatchingActiveAccountAndSession() {
        assertThatThrownBy(() -> service.assertActive(user.getId(), session.getId())).isInstanceOf(AuthException.class);
        when(sessions.existsActive(user.getId(), session.getId(), NOW, UserStatus.ACTIVE)).thenReturn(true);
        assertThatCode(() -> service.assertActive(user.getId(), session.getId())).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertActive(user.getId(), null)).isInstanceOf(AuthException.class);
    }
}
