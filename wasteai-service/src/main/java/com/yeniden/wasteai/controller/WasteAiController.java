package com.yeniden.wasteai.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import com.yeniden.wasteai.service.WasteClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Waste AI Service REST API Uç Noktaları (Port 8086).
 */
@RestController
@RequestMapping("/api/v1/wasteai")
@RequiredArgsConstructor
public class WasteAiController {

    private final WasteClassifier wasteClassifier;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Waste AI Service (Stub Adapter) çalışıyor!"));
    }

    @PostMapping("/classify")
    public ResponseEntity<ApiResponse<ClassifyImageResponse>> classifyImage(@RequestBody ClassifyImageRequest request) {
        ClassifyImageResponse response = wasteClassifier.classify(request);
        return ResponseEntity.ok(ApiResponse.success("Görüntü başarıyla analiz edildi", response));
    }
}
