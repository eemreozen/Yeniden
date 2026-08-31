package com.yeniden.identity.auth;

import com.yeniden.identity.domain.User;
import java.util.UUID;

public interface AuthSessionService {
    /** Caller must verify phone ownership and persist the user in the same transaction first. */
    TokenResponse issue(User user, boolean isNewUser);
    TokenResponse refresh(String raw);
    void logout(String raw);
    void assertActive(UUID userId, UUID sessionId);
}
