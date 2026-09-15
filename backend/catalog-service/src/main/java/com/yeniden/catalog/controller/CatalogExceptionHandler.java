package com.yeniden.catalog.controller;

import com.yeniden.common.exception.BaseException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Catalog Service için merkezi hata dönüştürücü. BaseException'ı gerçek HTTP statüsüne
 * ve problem+json gövdesine çevirir; bu olmadan tüm iş hataları varsayılan 500'e düşerdi.
 */
@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(BaseException.class)
    ResponseEntity<ProblemDetail> business(BaseException ex) {
        return response(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalid(Exception ex) {
        return response(400, "INVALID_REQUEST", "İstek alanlarını kontrol edin");
    }

    private ResponseEntity<ProblemDetail> response(int status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        body.setTitle(code);
        body.setProperty("code", code);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
