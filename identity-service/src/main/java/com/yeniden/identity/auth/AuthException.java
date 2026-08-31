package com.yeniden.identity.auth;

import com.yeniden.common.exception.BaseException;

public final class AuthException extends BaseException {
    private AuthException(String code, String message) {
        super(message, code, 401);
    }

    public static AuthException unauthorized() {
        return new AuthException("unauthorized", "Oturum veya kimlik bilgileri geçersiz.");
    }

    public static AuthException refreshReused() {
        return new AuthException("refresh_token_reused", "Güvenlik nedeniyle tüm oturumlar kapatıldı.");
    }
}
