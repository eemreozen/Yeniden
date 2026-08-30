package com.yeniden.identity.auth;

/** Bearer credentials: never log this response. expiresIn is in seconds. */
public record TokenResponse(String accessToken, String refreshToken, boolean isNewUser, long expiresIn) {
    @Override
    public String toString() {
        return "TokenResponse[credentials=REDACTED, isNewUser=" + isNewUser + ", expiresIn=" + expiresIn + "]";
    }
}
