package com.yeniden.identity.service;

import com.yeniden.identity.auth.TokenResponse;
import com.yeniden.identity.dto.PublicUserDto;
import com.yeniden.identity.dto.UpdateProfileRequest;
import com.yeniden.identity.dto.UserDto;
import java.util.UUID;

public interface UserService {
    TokenResponse loginVerifiedPhone(String normalizedPhone);
    UserDto getMe(UUID id);
    UserDto updateProfile(UUID id, UpdateProfileRequest request);
    PublicUserDto getPublicProfile(UUID id);
}
