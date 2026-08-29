package com.yeniden.identity.service;

import com.yeniden.identity.dto.RegisterRequest;
import com.yeniden.identity.dto.UserDto;

import java.util.UUID;

/**
 * Kullanıcı işlemlerinin iş mantığı (Business Logic) sözleşmesi.
 */
public interface UserService {
    UserDto registerOrLogin(RegisterRequest request);
    UserDto getUserById(UUID id);
    UserDto getUserByPhone(String phone);
}
