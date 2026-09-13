package com.yeniden.wasteai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfidenceCalculatorTest {

    @Test
    @DisplayName("Yüksek kesinlikli logprob (-0.05) ~0.95 olasılık vermelidir")
    void testHighConfidenceLogprob() {
        double result = ConfidenceCalculator.fromLogprob(-0.051293);
        assertEquals(0.95, result, 0.01);
        assertTrue(ConfidenceCalculator.isConfident(result));
    }

    @Test
    @DisplayName("Kararsız logprob (-0.69) ~0.50 olasılık vermeli ve %75 eşiğinin altında kalmalıdır")
    void testLowConfidenceLogprob() {
        double result = ConfidenceCalculator.fromLogprob(-0.693147);
        assertEquals(0.50, result, 0.01);
        assertFalse(ConfidenceCalculator.isConfident(result));
    }

    @Test
    @DisplayName("Null logprob varsayılan kalibrasyon değerini (0.85) döndürmelidir")
    void testNullLogprob() {
        double result = ConfidenceCalculator.fromLogprob(null);
        assertEquals(ConfidenceCalculator.DEFAULT_FALLBACK_CONFIDENCE, result);
        assertTrue(ConfidenceCalculator.isConfident(result));
    }

    @Test
    @DisplayName("Aşırı uç değerler [0.0, 1.0] aralığında sınırlandırılmalıdır")
    void testClamping() {
        assertEquals(1.0, ConfidenceCalculator.fromLogprob(0.5)); // Pozitif logprob olamaz ama sınır testi
        assertEquals(0.0, ConfidenceCalculator.fromLogprob(-100.0));
    }
}
