package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WasteAIServiceTest {

    private WasteClassifierStubImpl wasteClassifier;

    @BeforeEach
    void setUp() {
        wasteClassifier = new WasteClassifierStubImpl();
    }

    @Test
    @DisplayName("Karton kutu içeren resmi sınıflandırma testi")
    void classifyImage_CartonBox_Success() {
        ClassifyImageRequest request = new ClassifyImageRequest();
        request.setImageUrl("https://example.com/box.jpg");

        ClassifyImageResponse result = wasteClassifier.classify(request);

        assertNotNull(result);
        assertEquals("CARTON_BOX", result.getSuggestedCategoryCode());
        assertEquals(30, result.getEstimatedPoints());
    }

    @Test
    @DisplayName("Cam şişe içeren resmi sınıflandırma testi")
    void classifyImage_Glass_Success() {
        ClassifyImageRequest request = new ClassifyImageRequest();
        request.setImageUrl("https://example.com/cam-sise.jpg");

        ClassifyImageResponse result = wasteClassifier.classify(request);

        assertNotNull(result);
        assertEquals("GLASS", result.getSuggestedCategoryCode());
        assertEquals(20, result.getEstimatedPoints());
    }
}
