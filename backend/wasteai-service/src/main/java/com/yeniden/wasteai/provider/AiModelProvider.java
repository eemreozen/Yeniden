package com.yeniden.wasteai.provider;

import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;

/**
 * SOLID - ISP & DIP:
 * Farklı yapay zeka sağlayıcıları (Bulut API, Yerel Runtime, Kural Tabanlı Fallback)
 * için ortak soyutlama kontratı.
 */
public interface AiModelProvider {

    /**
     * Verilen görüntü isteğini analiz eder ve standart DTO çıktısını üretir.
     */
    ClassifyImageResponse analyze(ClassifyImageRequest request);

    /**
     * Sağlayıcının anlık olarak aktif ve ulaşılabilir olup olmadığını bildirir.
     */
    boolean isAvailable();

    /**
     * Sağlayıcının tanınabilir jenerik adı (Örn: "REMOTE_API", "LOCAL_MODEL", "RULE_FALLBACK").
     */
    String getProviderName();

    /**
     * Öncelik sırası (Düşük sayı = yüksek öncelik).
     */
    int getOrder();
}
