package com.yeniden.wasteai.provider;

import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import com.yeniden.wasteai.dto.DetectedItemDto;
import com.yeniden.wasteai.dto.EcocoinEstimateDto;
import com.yeniden.wasteai.dto.RecycleFallbackDto;
import com.yeniden.wasteai.service.ConfidenceCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SOLID - SRP & LSP:
 * Harici API ve Yerel GPU motorları ulaşılamaz olduğunda veya hata verdiğinde
 * uygulamanın akışını kesintisiz devam ettiren kural tabanlı acil durum sağlayıcısı.
 */
@Slf4j
@Component
public class RuleBasedFallbackProvider implements AiModelProvider {

    @Override
    public ClassifyImageResponse analyze(ClassifyImageRequest request) {
        log.info("RuleBasedFallbackProvider devreye girdi. Görüntü: {}", request.getImageUrl());

        String url = request.getImageUrl() != null ? request.getImageUrl().toLowerCase() : "";

        String itemName = "Oluklu Mukavva Koli Kutusu";
        String category = "CARDBOARD_BOX";
        String conditionGrade = "GOOD";
        boolean isDamaged = false;
        ActionType action = ActionType.REUSE;
        String reason = "Kutu yapısı sağlam, ikinci el ambalaj paylaşımı için uygun.";
        int basePoints = 10;
        double multiplier = 1.0;
        int reward = 15;
        boolean requiresRecycle = false;
        String nearestHub = null;

        // Basit dosya/url kural çıkarımları
        if (url.contains("cam") || url.contains("glass") || url.contains("sise") || url.contains("kavanoz")) {
            itemName = "Cam Kavanoz / Şişe";
            category = "GLASS";
            multiplier = 1.2;
            reward = 20;
            reason = "Cam kap sağlam ve yıkanabilir durumda. Tekrar kullanıma uygun.";
        } else if (url.contains("ahsap") || url.contains("wood") || url.contains("palet")) {
            itemName = "Ahşap Palet / Çıta";
            category = "WOOD";
            multiplier = 1.8;
            reward = 35;
            reason = "Ahşap malzeme dayanıklı, hobi ve onarım projeleri için yeniden değerlendirilebilir.";
        } else if (url.contains("kirik") || url.contains("cop") || url.contains("hasarli") || url.contains("broken") || url.contains("trash")) {
            itemName = "Deforme Olmuş Ambalaj Atığı";
            category = "RECYCLE_WASTE";
            conditionGrade = "POOR_RECYCLE";
            isDamaged = true;
            action = ActionType.RECYCLE;
            reason = "Malzeme fiziksel bütünlüğünü kaybetmiş. En yakın belediye atık toplama noktasına yönlendirildi.";
            multiplier = 0.5;
            reward = 5;
            requiresRecycle = true;
            nearestHub = "Belediye Ambalaj Atığı Toplama Noktası";
        }

        return ClassifyImageResponse.builder()
                .status("FALLBACK")
                .confidence(ConfidenceCalculator.DEFAULT_FALLBACK_CONFIDENCE)
                .detectedItem(DetectedItemDto.builder()
                        .name(itemName)
                        .category(category)
                        .conditionGrade(conditionGrade)
                        .isDamaged(isDamaged)
                        .suggestedAction(action)
                        .actionReason(reason)
                        .build())
                .ecocoinEstimate(EcocoinEstimateDto.builder()
                        .basePoints(basePoints)
                        .categoryMultiplier(multiplier)
                        .estimatedReward(reward)
                        .build())
                .recycleFallback(RecycleFallbackDto.builder()
                        .requiresRecyclePoint(requiresRecycle)
                        .nearestHubCategory(nearestHub)
                        .build())
                .build();
    }

    @Override
    public boolean isAvailable() {
        return true; // Kural tabanlı yedek motoru her zaman hazırdır
    }

    @Override
    public String getProviderName() {
        return "RULE_BASED_FALLBACK";
    }

    @Override
    public int getOrder() {
        return 100; // En düşük öncelik
    }
}
