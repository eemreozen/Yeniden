package com.yeniden.wasteai.provider;

import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedFallbackProviderTest {

    private RuleBasedFallbackProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RuleBasedFallbackProvider();
    }

    @Test
    @DisplayName("Kural sağlayıcısı varsayılan koli kutusu analizini doğru üretmelidir")
    void testAnalyze_DefaultBox() {
        ClassifyImageRequest request = ClassifyImageRequest.builder()
                .imageUrl("https://storage.yeniden.com/items/box123.jpg")
                .build();

        ClassifyImageResponse response = provider.analyze(request);

        assertNotNull(response);
        assertEquals("FALLBACK", response.getStatus());
        assertEquals("CARDBOARD_BOX", response.getDetectedItem().getCategory());
        assertEquals(ActionType.REUSE, response.getDetectedItem().getSuggestedAction());
        assertFalse(response.getDetectedItem().isDamaged());
        assertEquals(15, response.getEcocoinEstimate().getEstimatedReward());
        assertFalse(response.getRecycleFallback().isRequiresRecyclePoint());
    }

    @Test
    @DisplayName("Hasarlı veya çöp ibareli görselde RECYCLE kararı ve toplama noktası önerilmelidir")
    void testAnalyze_DamagedTrash() {
        ClassifyImageRequest request = ClassifyImageRequest.builder()
                .imageUrl("https://storage.yeniden.com/items/kirik_kutu.jpg")
                .build();

        ClassifyImageResponse response = provider.analyze(request);

        assertNotNull(response);
        assertEquals(ActionType.RECYCLE, response.getDetectedItem().getSuggestedAction());
        assertTrue(response.getDetectedItem().isDamaged());
        assertTrue(response.getRecycleFallback().isRequiresRecyclePoint());
        assertNotNull(response.getRecycleFallback().getNearestHubCategory());
    }

    @Test
    @DisplayName("Sağlayıcı özellikleri (Order 100, Always Available)")
    void testProviderContract() {
        assertTrue(provider.isAvailable());
        assertEquals(100, provider.getOrder());
        assertEquals("RULE_BASED_FALLBACK", provider.getProviderName());
    }
}
