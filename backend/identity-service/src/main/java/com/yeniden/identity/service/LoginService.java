package com.yeniden.identity.service;

import com.yeniden.identity.auth.TokenResponse;
import com.yeniden.identity.otp.OtpService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final OtpService otp;
    private final UserService users;

    public TokenResponse verify(String phone, String code, UUID challengeId) {
        // Redis consumption cannot participate in the PostgreSQL transaction.
        // A later database failure leaves the challenge spent; request a new OTP.
        String normalizedPhone = otp.verify(phone, code, challengeId);
        return users.loginVerifiedPhone(normalizedPhone);
    }
}

