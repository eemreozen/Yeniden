package com.yeniden.exchange.domain;

/**
 * Teslimat sürecinin durumları ( ADR-005 ).
 */
public enum HandoverStatus {
    PENDING_CODE,
    CONFIRMED,
    EXPIRED,
    NO_SHOW,
    PENDING_REVIEW
}
