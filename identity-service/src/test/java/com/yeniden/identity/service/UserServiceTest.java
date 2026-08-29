package com.yeniden.identity.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.domain.TrustScore;
import com.yeniden.identity.domain.User;
import com.yeniden.identity.domain.UserStatus;
import com.yeniden.identity.dto.RegisterRequest;
import com.yeniden.identity.dto.UserDto;
import com.yeniden.identity.repository.TrustScoreRepository;
import com.yeniden.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrustScoreRepository trustScoreRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private User user;
    private TrustScore trustScore;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setPhone("05551112233");
        registerRequest.setDisplayName("Ahmet Yılmaz");
        registerRequest.setEmail("ahmet@example.com");

        UUID userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .phone("05551112233")
                .displayName("Ahmet Yılmaz")
                .email("ahmet@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        trustScore = TrustScore.builder()
                .userId(userId)
                .score(50)
                .build();
    }

    @Test
    @DisplayName("Yeni kullanıcı kayıt/giriş yapma senaryosu")
    void registerOrLogin_NewUser_Success() {
        when(userRepository.findByPhone("05551112233")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(trustScoreRepository.save(any(TrustScore.class))).thenReturn(trustScore);

        UserDto result = userService.registerOrLogin(registerRequest);

        assertNotNull(result);
        assertEquals("05551112233", result.getPhone());
        assertEquals("Ahmet Yılmaz", result.getDisplayName());
        assertEquals(50, result.getTrustScore());

        verify(userRepository).save(any(User.class));
        verify(trustScoreRepository).save(any(TrustScore.class));
    }

    @Test
    @DisplayName("Var olan telefon ile giriş yapma senaryosu")
    void registerOrLogin_ExistingUser_Success() {
        when(userRepository.findByPhone("05551112233")).thenReturn(Optional.of(user));
        when(trustScoreRepository.findById(user.getId())).thenReturn(Optional.of(trustScore));

        UserDto result = userService.registerOrLogin(registerRequest);

        assertNotNull(result);
        assertEquals("05551112233", result.getPhone());
        assertEquals(50, result.getTrustScore());
    }

    @Test
    @DisplayName("ID ile var olan kullanıcıyı başarıyla getirme")
    void getUserById_Success() {
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(trustScoreRepository.findById(userId)).thenReturn(Optional.of(trustScore));

        UserDto result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(50, result.getTrustScore());
    }

    @Test
    @DisplayName("Var olmayan ID ile kullanıcı arandığında NOT_FOUND fırlatmalı")
    void getUserById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () -> userService.getUserById(userId));

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());
    }
}
