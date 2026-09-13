package com.yeniden.wasteai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalVisionModelProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private LocalVisionModelProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalVisionModelProvider(restTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("Yerel Ollama Qwen2-VL JSON yanıtı başarıyla çözümlenmelidir")
    void testAnalyze_Success() {
        String ollamaJson = """
            {
              "message": {
                "role": "assistant",
                "content": "{\\"name\\": \\"Oluklu Mukavva Koli Kutusu\\", \\"category\\": \\"CARDBOARD_BOX\\", \\"condition_grade\\": \\"GOOD\\", \\"is_damaged\\": false, \\"suggested_action\\": \\"REUSE\\", \\"action_reason\\": \\"Kutu yapısı sağlam.\\", \\"confidence\\": 0.94}"
              }
            }
            """;

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(ollamaJson, HttpStatus.OK));

        ClassifyImageRequest request = ClassifyImageRequest.builder()
                .imageBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .build();

        ClassifyImageResponse response = provider.analyze(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("CARDBOARD_BOX", response.getDetectedItem().getCategory());
        assertEquals(ActionType.REUSE, response.getDetectedItem().getSuggestedAction());
        assertEquals(0.94, response.getConfidence(), 0.001);
    }

    @Test
    @DisplayName("Yerel model servisi kapalıyken RestClientException fırlatmalıdır")
    void testAnalyze_ConnectionFailure() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        ClassifyImageRequest request = ClassifyImageRequest.builder().build();

        assertThrows(RuntimeException.class, () -> provider.analyze(request));
    }
}
