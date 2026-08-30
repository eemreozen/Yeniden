package com.yeniden.identity.controller;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.otp.RateLimitedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class IdentityExceptionHandler {
    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> limited(RateLimitedException ex) {
        return ResponseEntity.status(429).header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .cacheControl(CacheControl.noStore()).body(problem(429, ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(BaseException.class)
    ResponseEntity<ProblemDetail> business(BaseException ex) {
        return response(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalid(Exception ex) {
        return response(400, "invalid_request", "İstek alanlarını kontrol edin");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException ex) {
        return response(409, "conflict", "İşlem çakıştı; yeni doğrulama isteği oluşturun");
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<ProblemDetail> unavailable(Exception ex) {
        return response(503, "service_unavailable", "Hizmet geçici olarak kullanılamıyor");
    }

    private ResponseEntity<ProblemDetail> response(int status, String code, String detail) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(problem(status, code, detail));
    }
    private ProblemDetail problem(int status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        body.setTitle(code);
        body.setProperty("code", code);
        return body;
    }
}

