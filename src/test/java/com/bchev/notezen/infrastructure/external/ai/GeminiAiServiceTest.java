package com.bchev.notezen.infrastructure.external.ai;

import com.bchev.notezen.domain.model.LightReview;
import com.bchev.notezen.domain.model.StarRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GeminiAiServiceTest {

    private RestTemplate restTemplate;
    private GeminiAiService geminiAiService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        geminiAiService = new GeminiAiService();
        ReflectionTestUtils.setField(geminiAiService, "API_KEY", "dummy-key");
        ReflectionTestUtils.setField(geminiAiService, "restTemplate", restTemplate);
    }

    private LightReview review(String comment) {
        return new LightReview("locations/1/reviews/1", "Alice", StarRating.FIVE, comment);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> geminiResponse(String text) {
        Map<String, Object> body = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", text))
                        ))
                )
        );
        return ResponseEntity.ok(body);
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestResponse_withSuccessfulCall_shouldReturnGeneratedText() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(geminiResponse("Merci pour votre avis !"));

        String result = geminiAiService.suggestResponse(review("Super endroit"));

        assertEquals("Merci pour votre avis !", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestResponse_withNullComment_shouldStillCallApi() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(geminiResponse("Merci pour la note !"));

        String result = geminiAiService.suggestResponse(review(null));

        assertEquals("Merci pour la note !", result);
        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestResponse_withResponseMissingCandidates_shouldReturnGenericErrorMessage() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("no-candidates-here", true)));

        String result = geminiAiService.suggestResponse(review("Comment"));

        assertEquals("Erreur lors de la génération.", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestResponse_withNonRetryableException_shouldReturnFallbackWithoutRetry() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("boom"));

        String result = geminiAiService.suggestResponse(review("Comment"));

        assertEquals("L'assistant IA est temporairement surchargé. Veuillez réessayer dans un instant.", result);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestResponse_with503ThenSuccess_shouldRetryAndReturnGeneratedText() {
        HttpServerErrorException.ServiceUnavailable unavailable =
                (HttpServerErrorException.ServiceUnavailable) HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        new HttpHeaders(), new byte[0], null);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(unavailable)
                .thenReturn(geminiResponse("Merci après réessai !"));

        String result = geminiAiService.suggestResponse(review("Comment"));

        assertEquals("Merci après réessai !", result);
        verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(Map.class));
    }
}
