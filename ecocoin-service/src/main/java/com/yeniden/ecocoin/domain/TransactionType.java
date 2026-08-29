package com.yeniden.ecocoin.domain;

/**
 * Eco-Coin Double-Entry İşlem Tipleri (04-ecocoin-rules.md).
 */
public enum TransactionType {
    GRANT,       // Teslimat veya görev sonucu puan kazanımı
    HOLD,        // Harcama sırasında puanın geçici dondurulması
    CAPTURE,     // Dondurulan puanın partner tarafından çekilmesi (yakılması)
    RELEASE,     // Dondurulan puanın kullanıcıya iadesi
    ADJUSTMENT   // Moderatör müdahalesi / sahtecilik geri alma
}
