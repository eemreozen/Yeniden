package com.yeniden.identity.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.identity.dto.*;
import com.yeniden.identity.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(users.getMe(UUID.fromString(jwt.getSubject())));
    }

    @PatchMapping("/me")
    public ApiResponse<UserDto> update(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(users.updateProfile(UUID.fromString(jwt.getSubject()), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicUserDto> profile(@PathVariable UUID id) {
        return ApiResponse.success(users.getPublicProfile(id));
    }

    /** Downstream services use this immutable timestamp for account-age rules. */
    @GetMapping("/{id}/account-created-at")
    public ApiResponse<AccountCreatedAtDto> accountCreatedAt(@PathVariable UUID id) {
        return ApiResponse.success(users.getAccountCreatedAt(id));
    }
}
