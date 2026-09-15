package com.yeniden.wasteai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RemoteVisionApiProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private RemoteVisionApiProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RemoteVisionApiProvider(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "apiKey", "test-api-key");
    }

    @Test
    @DisplayName("Bulut API markdown JSON ve logprob yanıtını doğru parse etmelidir")
    void testParseRemoteResponse_WithMarkdownAndLogprob() throws Exception {
        String geminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\\n{\\n  \\"name\\": \\"Oluklu Mukavva Koli Kutusu\\",\\n  \\"category\\": \\"CARDBOARD_BOX\\",\\n  \\"condition_grade\\": \\"GOOD\\",\\n  \\"is_damaged\\": false,\\n  \\"suggested_action\\": \\"REUSE\\",\\n  \\"action_reason\\": \\"Kutu yapısı sağlam.\\"\\n}\\n```"
                      }
                    ]
                  },
                  "logprobsResult": {
                    "avgLogprob": -0.051293
                  }
                }
              ]
            }
            """;

        ClassifyImageResponse response = provider.parseRemoteResponse(geminiJson);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("CARDBOARD_BOX", response.getDetectedItem().getCategory());
        assertEquals(ActionType.REUSE, response.getDetectedItem().getSuggestedAction());
        assertEquals(0.95, response.getConfidence(), 0.01);
    }

    @Test
    @DisplayName("API Key eksik olduğunda isAvailable false dönmelidir")
    void testIsAvailable_NoApiKey() {
        ReflectionTestUtils.setField(provider, "apiKey", "");
        assertFalse(provider.isAvailable());
    }
}
