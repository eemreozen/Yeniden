package com.yeniden.identity.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.auth.*;
import com.yeniden.identity.domain.*;
import com.yeniden.identity.dto.UpdateProfileRequest;
import com.yeniden.identity.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    UserRepository users = mock(UserRepository.class);
    TrustScoreRepository scores = mock(TrustScoreRepository.class);
    AuthUserLockRepository locks = mock(AuthUserLockRepository.class);
    AuthSessionService sessions = mock(AuthSessionService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"), ZoneOffset.UTC);
    UserServiceImpl service = new UserServiceImpl(users, scores, locks, sessions, clock);
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).phone("+905551234567").displayName("Ada")
            .createdAt(clock.instant()).status(UserStatus.ACTIVE).build();

    @Test void verifiedRegistrationPersistsBeforeIssuingSession() {
        when(users.findByPhone(user.getPhone())).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(call -> {
            User created = call.getArgument(0);
            created.setId(id);
            assertThat(created.getPhoneVerifiedAt()).isEqualTo(clock.instant());
            assertThat(created.getRoles()).containsExactly(UserRole.USER);
            return created;
        });
        service.loginVerifiedPhone(user.getPhone());
        var order = inOrder(users, scores, sessions);
        order.verify(users).findByPhone(user.getPhone());
        order.verify(users).saveAndFlush(any());
        order.verify(scores).saveAndFlush(argThat(score -> score.getScore() == 50 && score.getUserId().equals(id)));
        order.verify(sessions).issue(argThat(u -> u.getId().equals(id)), eq(true));
    }

    @Test void existingUserIsLockedAndReverifiedWithoutCreatingAnotherTrustRow() {
        when(users.findByPhone(user.getPhone())).thenReturn(Optional.of(user));
        when(locks.lockById(id)).thenReturn(Optional.of(user));
        service.loginVerifiedPhone(user.getPhone());
        assertThat(user.getPhoneVerifiedAt()).isEqualTo(clock.instant());
        verify(sessions).issue(user, false);
        verifyNoInteractions(scores);
    }

    @Test void inactiveAccountsCannotLoginOrUpdate() {
        for (UserStatus state : List.of(UserStatus.SUSPENDED, UserStatus.DELETED)) {
            user.setStatus(state);
            when(users.findByPhone(user.getPhone())).thenReturn(Optional.of(user));
            when(locks.lockById(id)).thenReturn(Optional.of(user));
            assertThatThrownBy(() -> service.loginVerifiedPhone(user.getPhone())).isInstanceOf(BaseException.class);
            assertThatThrownBy(() -> service.updateProfile(id, new UpdateProfileRequest("New name", null)))
                    .isInstanceOf(BaseException.class);
        }
        verifyNoInteractions(sessions);
    }

    @Test void publicDtoHasOnlyPublicFields() {
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(scores.findById(id)).thenReturn(Optional.of(TrustScore.builder().userId(id).score(50).build()));
        var dto = service.getPublicProfile(id);
        assertThat(dto.displayName()).isEqualTo("Ada");
        assertThat(Arrays.stream(dto.getClass().getRecordComponents()).map(c -> c.getName()).toList())
                .containsExactly("id", "displayName", "avatarKey", "trustScore");
    }

    @Test void missingTrustIsNotReportedAsFabricatedScore() {
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(scores.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMe(id)).isInstanceOf(BaseException.class)
                .extracting("httpStatus").isEqualTo(503);
    }

    @Test void accountCreatedAtComesFromIdentityUserAndDoesNotDependOnWalletData() {
        when(users.findById(id)).thenReturn(Optional.of(user));

        var dto = service.getAccountCreatedAt(id);

        assertThat(dto.userId()).isEqualTo(id);
        assertThat(dto.accountCreatedAt()).isEqualTo(user.getCreatedAt());
        verify(users).findById(id);
        verifyNoInteractions(scores, locks, sessions);
    }

    @Test void profileUpdateUsesClockAndDoesNotChangePhone() {
        when(locks.lockById(id)).thenReturn(Optional.of(user));
        when(scores.findById(id)).thenReturn(Optional.of(TrustScore.builder().userId(id).score(50).build()));
        service.updateProfile(id, new UpdateProfileRequest(" Deniz ", "avatars/test.png"));
        assertThat(user.getDisplayName()).isEqualTo("Deniz");
        assertThat(user.getPhone()).isEqualTo("+905551234567");
        assertThat(user.getUpdatedAt()).isEqualTo(clock.instant());
    }
}
