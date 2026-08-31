package com.yeniden.identity.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.auth.AuthSessionService;
import com.yeniden.identity.auth.AuthUserLockRepository;
import com.yeniden.identity.auth.TokenResponse;
import com.yeniden.identity.domain.*;
import com.yeniden.identity.dto.*;
import com.yeniden.identity.repository.*;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final TrustScoreRepository scores;
    private final AuthUserLockRepository locks;
    private final AuthSessionService sessions;
    private final Clock clock;

    /** Only the OTP orchestration service may invoke this after atomic consumption. */
    @Override @Transactional
    public TokenResponse loginVerifiedPhone(String phone) {
        User user = users.findByPhone(phone).orElse(null);
        boolean isNew = user == null;
        if (isNew) {
            user = User.builder().phone(phone).displayName("Yeni Kullanıcı")
                    .phoneVerifiedAt(clock.instant()).status(UserStatus.ACTIVE)
                    .createdAt(clock.instant()).updatedAt(clock.instant()).build();
            users.saveAndFlush(user);
            scores.saveAndFlush(TrustScore.builder().userId(user.getId())
                    .score(50).updatedAt(clock.instant()).build());
        } else {
            user = locks.lockById(user.getId()).orElseThrow(this::notFound);
            requireActive(user);
            user.setPhoneVerifiedAt(clock.instant());
            user.setUpdatedAt(clock.instant());
            users.saveAndFlush(user);
        }
        return sessions.issue(user, isNew);
    }

    @Override @Transactional(readOnly = true)
    public UserDto getMe(UUID id) {
        User user = users.findById(id).orElseThrow(this::notFound);
        requireActive(user);
        return privateDto(user);
    }

    @Override @Transactional
    public UserDto updateProfile(UUID id, UpdateProfileRequest request) {
        User user = locks.lockById(id).orElseThrow(this::notFound);
        requireActive(user);
        if (request.displayName() != null) user.setDisplayName(request.displayName().strip());
        if (request.avatarKey() != null) user.setAvatarKey(request.avatarKey());
        user.setUpdatedAt(clock.instant());
        users.saveAndFlush(user);
        return privateDto(user);
    }

    @Override @Transactional(readOnly = true)
    public PublicUserDto getPublicProfile(UUID id) {
        User user = users.findById(id).orElseThrow(this::notFound);
        if (user.getStatus() != UserStatus.ACTIVE) throw notFound();
        return new PublicUserDto(user.getId(), user.getDisplayName(), user.getAvatarKey(), score(id));
    }

    private int score(UUID id) {
        return scores.findById(id).map(TrustScore::getScore)
                .orElseThrow(() -> new BaseException("Profil geçici olarak kullanılamıyor", "service_unavailable", 503));
    }

    private UserDto privateDto(User user) {
        return UserDto.builder().id(user.getId()).phone(user.getPhone()).email(user.getEmail())
                .displayName(user.getDisplayName()).avatarKey(user.getAvatarKey())
                .neighborhoodId(user.getNeighborhoodId()).status(user.getStatus())
                .trustScore(score(user.getId())).createdAt(user.getCreatedAt()).build();
    }

    private void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE)
            throw new BaseException("Hesap kullanılamıyor", "account_unavailable", 403);
    }

    private BaseException notFound() {
        return new BaseException("Kullanıcı bulunamadı", "user_not_found", 404);
    }
}
