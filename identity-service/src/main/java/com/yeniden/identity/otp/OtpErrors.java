package com.yeniden.identity.otp;

import com.yeniden.common.exception.BaseException;

final class OtpErrors {
    private OtpErrors() { }

    static BaseException invalid() {
        return new BaseException("Geçersiz veya süresi dolmuş doğrulama isteği.", "invalid_otp", 400);
    }

    static BaseException unavailable() {
        // Never attach provider/Redis errors: those may contain phone numbers or credentials.
        return new BaseException("Doğrulama hizmeti şu anda kullanılamıyor.", "service_unavailable", 503);
    }
}
