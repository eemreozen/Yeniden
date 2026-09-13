package com.yeniden.wasteai.service;

/**
 * SOLID - SRP:
 * Yalnızca logprob (log olasılık) değerlerini matematiksel gerçek olasılığa
 * (P = exp(logprob)) dönüştürme ve güven skoru kalibrasyonundan sorumludur.
 */
public final class ConfidenceCalculator {

    public static final double DEFAULT_FALLBACK_CONFIDENCE = 0.85;
    public static final double MIN_ACCEPTABLE_CONFIDENCE = 0.75;

    private ConfidenceCalculator() {
        // Utility sınıfı
    }

    /**
     * Logprob değerinden (örn: -0.05) olasılık yüzdesi (örn: 0.95) türetir.
     * P = e^(logprob)
     *
     * @param logprob Modelden dönen log olasılık değeri
     * @return 0.0 ile 1.0 arasında yuvarlanmış güven skoru
     */
    public static double fromLogprob(Double logprob) {
        if (logprob == null) {
            return DEFAULT_FALLBACK_CONFIDENCE;
        }
        double prob = Math.exp(logprob);
        // [0.0, 1.0] aralığında sınırla ve 2 ondalık basamağa yuvarla
        double clamped = Math.min(1.0, Math.max(0.0, prob));
        return Math.round(clamped * 100.0) / 100.0;
    }

    /**
     * Güven skorunun kabul edilebilir eşiğin (%75) üzerinde olup olmadığını doğrular.
     */
    public static boolean isConfident(double confidence) {
        return confidence >= MIN_ACCEPTABLE_CONFIDENCE;
    }
}
