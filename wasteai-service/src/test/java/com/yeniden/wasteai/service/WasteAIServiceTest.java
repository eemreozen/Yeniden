package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WasteAIServiceTest {

    private WasteClassifierStubImpl wasteClassifier;

    @BeforeEach
    void setUp() {
        wasteClassifier = new WasteClassifierStubImpl();
    }

    @Test
    @DisplayName("Karton kutu içeren resmi sınıflandırma testi (Yeni ve Eski Metot Uyumu)")
    void classifyImage_CartonBox_Success() {
        ClassifyImageRequest request = new ClassifyImageRequest();
        request.setImageUrl("https://example.com/box.jpg");

        ClassifyImageResponse result = wasteClassifier.classify(request);

        assertNotNull(result);
        assertEquals("CARDBOARD_BOX", result.getSuggestedCategoryCode());
        assertEquals("CARDBOARD_BOX", result.getDetectedItem().getCategory());
        assertEquals(ActionType.REUSE, result.getDetectedItem().getSuggestedAction());
        assertEquals(15, result.getEstimatedPoints());
        assertEquals(15, result.getEcocoinEstimate().getEstimatedReward());
        assertTrue(result.getConfidence() >= 0.75);
    }

    @Test
    @DisplayName("Cam şişe içeren resmi sınıflandırma testi")
    void classifyImage_Glass_Success() {
        ClassifyImageRequest request = new ClassifyImageRequest();
        request.setImageUrl("https://example.com/cam-sise.jpg");

        ClassifyImageResponse result = wasteClassifier.classify(request);

        assertNotNull(result);
        assertEquals("GLASS", result.getSuggestedCategoryCode());
        assertEquals("GLASS", result.getDetectedItem().getCategory());
        assertEquals(ActionType.REUSE, result.getDetectedItem().getSuggestedAction());
        assertEquals(20, result.getEstimatedPoints());
    }
}
