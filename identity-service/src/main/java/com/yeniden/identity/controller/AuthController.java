package com.yeniden.identity.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.identity.auth.AuthSessionService;
import com.yeniden.identity.auth.TokenResponse;
import com.yeniden.identity.otp.OtpChallengeResponse;
import com.yeniden.identity.otp.OtpService;
import com.yeniden.identity.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final OtpService otp;
    private final LoginService login;
    private final AuthSessionService sessions;

    public record OtpRequest(@NotBlank @Size(max = 40) String phone) {}
    public record VerifyRequest(@NotBlank @Size(max = 40) String phone,
                                @NotNull @Pattern(regexp = "[0-9]{6}") String code,
                                @NotNull UUID challengeId) {
        @Override public String toString() { return "VerifyRequest[REDACTED]"; }
    }
    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {
        @Override public String toString() { return "RefreshRequest[REDACTED]"; }
    }

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> request(
            @Valid @RequestBody OtpRequest request, HttpServletRequest http) {
        // Forwarded headers are deliberately not trusted. See deployment notes.
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(otp.request(request.phone(), http.getRemoteAddr())));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verify(@Valid @RequestBody VerifyRequest request) {
        return tokens(login.verify(request.phone(), request.code(), request.challengeId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return tokens(sessions.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        sessions.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private ResponseEntity<ApiResponse<TokenResponse>> tokens(TokenResponse tokens) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(tokens));
    }
}

