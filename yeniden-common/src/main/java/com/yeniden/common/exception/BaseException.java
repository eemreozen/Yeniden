package com.yeniden.common.exception;

import lombok.Getter;

/**
 * YENİDEN projesindeki tüm özel (custom) istisnaların atası olan temel hata sınıfı.
 * Java'nın RuntimeException (çalışma zamanı istisnası) sınıfından türetilmiştir.
 */
@Getter
public class BaseException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public BaseException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
