package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import com.yeniden.wasteai.provider.RuleBasedFallbackProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Geriye dönük uyumluluk adaptörü (RuleBasedFallbackProvider'a delege eder).
 */
@Service
@RequiredArgsConstructor
public class WasteClassifierStubImpl implements WasteClassifier {

    private final RuleBasedFallbackProvider fallbackProvider;

    public WasteClassifierStubImpl() {
        this.fallbackProvider = new RuleBasedFallbackProvider();
    }

    @Override
    public ClassifyImageResponse classify(ClassifyImageRequest request) {
        return fallbackProvider.analyze(request);
    }
}
