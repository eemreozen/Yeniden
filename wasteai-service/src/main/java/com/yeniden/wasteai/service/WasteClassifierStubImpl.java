package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import org.springframework.stereotype.Service;

/**
 * Waste AI Stub Adapter Gerçekleşimi (Faz 1 Mock Sınıflandırıcı).
 */
@Service
public class WasteClassifierStubImpl implements WasteClassifier {

    @Override
    public ClassifyImageResponse classify(ClassifyImageRequest request) {
        String url = request.getImageUrl() != null ? request.getImageUrl().toLowerCase() : "";

        if (url.contains("glass") || url.contains("cam")) {
            return ClassifyImageResponse.builder()
                    .suggestedCategoryCode("GLASS")
                    .suggestedCategoryName("Cam Şişe & Kavanoz")
                    .confidenceScore(0.92)
                    .estimatedPoints(20)
                    .description("Görüntü analiz edildi: Cam malzeme tespit edildi (%92 güven ilkesi).")
                    .build();
        }

        if (url.contains("electronic") || url.contains("elektronik")) {
            return ClassifyImageResponse.builder()
                    .suggestedCategoryCode("ELECTRONIC")
                    .suggestedCategoryName("Küçük Ev Aleti / Elektronik Atık")
                    .confidenceScore(0.98)
                    .estimatedPoints(50)
                    .description("Görüntü analiz edildi: Elektronik cihaz tespit edildi (%98 güven ilkesi).")
                    .build();
        }

        // Varsayılan Ambalaj / Karton Koli
        return ClassifyImageResponse.builder()
                .suggestedCategoryCode("CARTON_BOX")
                .suggestedCategoryName("Karton / Ambalaj Kutusu")
                .confidenceScore(0.95)
                .estimatedPoints(30)
                .description("Görüntü analiz edildi: Geri dönüştürülebilir ambalaj/koli tespit edildi (%95 güven ilkesi).")
                .build();
    }
}
