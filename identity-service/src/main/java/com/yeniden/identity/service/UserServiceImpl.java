package com.yeniden.identity.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.domain.TrustScore;
import com.yeniden.identity.domain.User;
import com.yeniden.identity.domain.UserStatus;
import com.yeniden.identity.dto.RegisterRequest;
import com.yeniden.identity.dto.UserDto;
import com.yeniden.identity.repository.TrustScoreRepository;
import com.yeniden.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Kullanıcı iş mantığının (Business Logic) gerçeklendiği servis katmanı.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TrustScoreRepository trustScoreRepository;

    @Override
    @Transactional
    public UserDto registerOrLogin(RegisterRequest request) {
        // Telefonla kayıtlı kullanıcı var mı kontrol et, yoksa oluştur
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> createNewUser(request));

        int score = trustScoreRepository.findById(user.getId())
                .map(TrustScore::getScore)
                .orElse(50);

        return mapToDto(user, score);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException("Kullanıcı bulunamadı!", "USER_NOT_FOUND", 404));

        int score = trustScoreRepository.findById(id)
                .map(TrustScore::getScore)
                .orElse(50);

        return mapToDto(user, score);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByPhone(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BaseException("Kullanıcı bulunamadı!", "USER_NOT_FOUND", 404));

        int score = trustScoreRepository.findById(user.getId())
                .map(TrustScore::getScore)
                .orElse(50);

        return mapToDto(user, score);
    }

    private User createNewUser(RegisterRequest request) {
        User newUser = User.builder()
                .phone(request.getPhone())
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : "Yeni Kullanıcı")
                .email(request.getEmail())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);

        // Yeni kullanıcı için başlangıç güven skoru (50) oluştur
        TrustScore initialTrust = TrustScore.builder()
                .userId(savedUser.getId())
                .score(50)
                .build();
        trustScoreRepository.save(initialTrust);

        return savedUser;
    }

    private UserDto mapToDto(User user, int trustScore) {
        return UserDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarKey(user.getAvatarKey())
                .neighborhoodId(user.getNeighborhoodId())
                .status(user.getStatus())
                .trustScore(trustScore)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
