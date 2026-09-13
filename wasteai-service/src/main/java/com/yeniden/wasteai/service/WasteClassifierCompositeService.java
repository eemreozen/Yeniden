package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import com.yeniden.wasteai.provider.AiModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * SOLID - DIP, OCP & SRP:
 * Tüm AiModelProvider'ları öncelik sırasına (getOrder) göre koordine eden,
 * sağlayıcı çöktüğünde veya hata verdiğinde sonraki sağlayıcıya devreden (Failover)
 * ana orkestrasyon servisi.
 *
 * Güven skoru %75'in altına düştüğünde nesneyi otomatik olarak moderatör onayına (requires_moderation=true) yönlendirir.
 */
@Slf4j
@Service
@Primary
public class WasteClassifierCompositeService implements WasteClassifier {

    private final List<AiModelProvider> providers;

    public WasteClassifierCompositeService(List<AiModelProvider> providers) {
        // Öncelik sırasına göre sırala (Order küçük olan önce denenir)
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(AiModelProvider::getOrder))
                .toList();
        log.info("WasteClassifierCompositeService yüklendi. Aktif Sağlayıcılar: {}",
                this.providers.stream().map(AiModelProvider::getProviderName).toList());
    }

    @Override
    public ClassifyImageResponse classify(ClassifyImageRequest request) {
        for (AiModelProvider provider : providers) {
            if (!provider.isAvailable()) {
                log.debug("Sağlayıcı {} şu anda aktif değil, atlanıyor.", provider.getProviderName());
                continue;
            }

            try {
                log.info("Görüntü analizi için [{}] sağlayıcısı deneniyor...", provider.getProviderName());
                ClassifyImageResponse response = provider.analyze(request);
                if (response != null) {
                    // Güven eşiği kontrolü (%75 barajı)
                    boolean lowConfidence = !ConfidenceCalculator.isConfident(response.getConfidence());
                    response.setRequiresModeration(lowConfidence);

                    if (lowConfidence && (response.getModerationReason() == null || response.getModerationReason().isBlank())) {
                        response.setModerationReason(String.format(
                                "Düşük yapay zeka güven skoru (%%%d): Kabul edilebilir eşiğin (%%75) altında kaldığı için moderatör onayı gerektirir.",
                                Math.round(response.getConfidence() * 100)
                        ));
                        log.warn("Görüntü güven skoru (%{}) %75 eşiğinin altında kaldı. Moderatör onayına sevk edildi.",
                                Math.round(response.getConfidence() * 100));
                    } else {
                        log.info("Sağlayıcı [{}] başarıyla yanıt verdi. Güven Skoru: {}",
                                provider.getProviderName(), response.getConfidence());
                    }

                    return response;
                }
            } catch (Exception e) {
                log.warn("Sağlayıcı [{}] çağrısı sırasında hata oluştu: {}. Sıradaki sağlayıcıya geçiliyor.",
                        provider.getProviderName(), e.getMessage());
            }
        }

        throw new IllegalStateException("Hiçbir yapay zeka sağlayıcısı yanıt üretemedi.");
    }
}
