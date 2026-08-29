package com.yeniden.catalog.domain;

/**
 * İlan yaşam döngüsü durumları: DRAFT -> PUBLISHED -> RESERVED -> HANDED_OVER -> CLOSED
 */
public enum ListingStatus {
    DRAFT,
    PUBLISHED,
    RESERVED,
    HANDED_OVER,
    CLOSED,
    EXPIRED,
    WITHDRAWN
}
