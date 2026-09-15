package com.yeniden.wasteai.service;

import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import com.yeniden.wasteai.dto.DetectedItemDto;
import com.yeniden.wasteai.provider.AiModelProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WasteClassifierCompositeServiceTest {

    @Mock
    private AiModelProvider remoteProvider;

    @Mock
    private AiModelProvider localProvider;

    @Mock
    private AiModelProvider fallbackProvider;

    @Test
    @DisplayName("Öncelikli sağlayıcı başarılı olduğunda sonraki sağlayıcılara gitmemelidir")
    void testPrimaryProviderSucceeds() {
        when(remoteProvider.getOrder()).thenReturn(10);
        when(localProvider.getOrder()).thenReturn(20);
        when(fallbackProvider.getOrder()).thenReturn(100);

        when(remoteProvider.isAvailable()).thenReturn(true);
        when(remoteProvider.getProviderName()).thenReturn("REMOTE_API");
        when(remoteProvider.analyze(any())).thenReturn(ClassifyImageResponse.builder()
                .status("SUCCESS")
                .confidence(0.95)
                .detectedItem(DetectedItemDto.builder()
                        .name("Koli")
                        .category("CARDBOARD_BOX")
                        .suggestedAction(ActionType.REUSE)
                        .build())
                .build());

        WasteClassifierCompositeService service = new WasteClassifierCompositeService(
                List.of(fallbackProvider, localProvider, remoteProvider)
        );

        ClassifyImageRequest request = ClassifyImageRequest.builder().build();
        ClassifyImageResponse response = service.classify(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertFalse(response.isRequiresModeration());
        verify(remoteProvider, times(1)).analyze(request);
        verify(localProvider, never()).analyze(any());
        verify(fallbackProvider, never()).analyze(any());
    }

    @Test
    @DisplayName("Dış API çöktüğünde otomatik olarak Yerel sağlayıcıya devretmelidir (Failover)")
    void testFailoverToLocalProvider() {
        when(remoteProvider.getOrder()).thenReturn(10);
        when(localProvider.getOrder()).thenReturn(20);
        when(fallbackProvider.getOrder()).thenReturn(100);

        when(remoteProvider.isAvailable()).thenReturn(true);
        when(remoteProvider.getProviderName()).thenReturn("REMOTE_API");
        when(remoteProvider.analyze(any())).thenThrow(new RuntimeException("Cloud API Rate Limit"));

        when(localProvider.isAvailable()).thenReturn(true);
        when(localProvider.getProviderName()).thenReturn("LOCAL_MODEL");
        when(localProvider.analyze(any())).thenReturn(ClassifyImageResponse.builder()
                .status("SUCCESS")
                .confidence(0.90)
                .detectedItem(DetectedItemDto.builder()
                        .name("Koli (Local)")
                        .category("CARDBOARD_BOX")
                        .suggestedAction(ActionType.REUSE)
                        .build())
                .build());

        WasteClassifierCompositeService service = new WasteClassifierCompositeService(
                List.of(remoteProvider, localProvider, fallbackProvider)
        );

        ClassifyImageRequest request = ClassifyImageRequest.builder().build();
        ClassifyImageResponse response = service.classify(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Koli (Local)", response.getDetectedItem().getName());
        assertFalse(response.isRequiresModeration());
        verify(remoteProvider, times(1)).analyze(request);
        verify(localProvider, times(1)).analyze(request);
        verify(fallbackProvider, never()).analyze(any());
    }

    @Test
    @DisplayName("Tüm yapay zeka motorları çöktüğünde kural tabanlı yedeğe düşmelidir")
    void testFailoverToRuleBasedFallback() {
        when(remoteProvider.getOrder()).thenReturn(10);
        when(localProvider.getOrder()).thenReturn(20);
        when(fallbackProvider.getOrder()).thenReturn(100);

        when(remoteProvider.isAvailable()).thenReturn(false);
        when(remoteProvider.getProviderName()).thenReturn("REMOTE_API");

        when(localProvider.isAvailable()).thenReturn(true);
        when(localProvider.getProviderName()).thenReturn("LOCAL_MODEL");
        when(localProvider.analyze(any())).thenThrow(new RuntimeException("Ollama offline"));

        when(fallbackProvider.isAvailable()).thenReturn(true);
        when(fallbackProvider.getProviderName()).thenReturn("RULE_FALLBACK");
        when(fallbackProvider.analyze(any())).thenReturn(ClassifyImageResponse.builder()
                .status("FALLBACK")
                .confidence(0.85)
                .build());

        WasteClassifierCompositeService service = new WasteClassifierCompositeService(
                List.of(remoteProvider, localProvider, fallbackProvider)
        );

        ClassifyImageRequest request = ClassifyImageRequest.builder().build();
        ClassifyImageResponse response = service.classify(request);

        assertNotNull(response);
        assertEquals("FALLBACK", response.getStatus());
        assertFalse(response.isRequiresModeration());
        verify(fallbackProvider, times(1)).analyze(request);
    }

    @Test
    @DisplayName("Güven skoru %75 altında kaldığında requiresModeration true olmalı ve gerekçe yazılmalıdır")
    void testLowConfidenceTriggersModeration() {
        when(remoteProvider.getOrder()).thenReturn(10);
        when(localProvider.getOrder()).thenReturn(20);
        when(fallbackProvider.getOrder()).thenReturn(100);

        when(remoteProvider.isAvailable()).thenReturn(true);
        when(remoteProvider.getProviderName()).thenReturn("REMOTE_API");
        when(remoteProvider.analyze(any())).thenReturn(ClassifyImageResponse.builder()
                .status("SUCCESS")
                .confidence(0.58)
                .detectedItem(DetectedItemDto.builder()
                        .name("Şüpheli / Belirsiz Nesne")
                        .category("UNKNOWN")
                        .suggestedAction(ActionType.REUSE)
                        .build())
                .build());

        WasteClassifierCompositeService service = new WasteClassifierCompositeService(
                List.of(fallbackProvider, localProvider, remoteProvider)
        );

        ClassifyImageRequest request = ClassifyImageRequest.builder().build();
        ClassifyImageResponse response = service.classify(request);

        assertNotNull(response);
        assertTrue(response.isRequiresModeration());
        assertNotNull(response.getModerationReason());
        assertTrue(response.getModerationReason().contains("%75"));
    }
}
