package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;

/**
 * Waste AI Görüntü Sınıflandırma Servis Arayüzü (Port-Adapter Pattern).
 */
public interface WasteClassifier {
    ClassifyImageResponse classify(ClassifyImageRequest request);
}
