package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealGoogleAuthServiceTest {

    private RestTemplate restTemplate;
    private RealGoogleAuthService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        service = new RealGoogleAuthService();
        ReflectionTestUtils.setField(service, "clientId", "client-123");
        ReflectionTestUtils.setField(service, "clientSecret", "secret-abc");
        ReflectionTestUtils.setField(service, "redirectUri", "https://app.example.com/api/google/callback");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exchangeCodeForTokens_shouldPostAuthorizationCodeGrantWithCredentials() {
        GoogleTokenResponseDTO expected = new GoogleTokenResponseDTO();
        expected.setAccessToken("access-token-123");
        expected.setIdToken("id-token-123");

        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(eq("https://oauth2.googleapis.com/token"),
                entityCaptor.capture(), eq(GoogleTokenResponseDTO.class)))
                .thenReturn(expected);

        GoogleTokenResponseDTO result = service.exchangeCodeForTokens("auth-code-xyz");

        assertSame(expected, result);
        MultiValueMap<String, String> body = entityCaptor.getValue().getBody();
        assertEquals("auth-code-xyz", body.getFirst("code"));
        assertEquals("client-123", body.getFirst("client_id"));
        assertEquals("secret-abc", body.getFirst("client_secret"));
        assertEquals("https://app.example.com/api/google/callback", body.getFirst("redirect_uri"));
        assertEquals("authorization_code", body.getFirst("grant_type"));
    }

    @Test
    void getAuthorizationUrl_shouldIncludeClientIdAndRedirectUri() {
        String url = service.getAuthorizationUrl();

        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertTrue(url.contains("client_id=client-123"));
        assertTrue(url.contains("redirect_uri=https://app.example.com/api/google/callback"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
    }

    @Test
    void refreshAccessToken_withValidResponse_shouldReturnNewAccessToken() {
        GoogleTokenResponseDTO response = new GoogleTokenResponseDTO();
        response.setAccessToken("new-access-token");
        when(restTemplate.postForObject(eq("https://oauth2.googleapis.com/token"), any(), eq(GoogleTokenResponseDTO.class)))
                .thenReturn(response);

        String token = service.refreshAccessToken("refresh-token-abc");

        assertEquals("new-access-token", token);
    }

    @Test
    void refreshAccessToken_withNullResponse_shouldReturnNull() {
        when(restTemplate.postForObject(eq("https://oauth2.googleapis.com/token"), any(), eq(GoogleTokenResponseDTO.class)))
                .thenReturn(null);

        String token = service.refreshAccessToken("refresh-token-abc");

        assertNull(token);
    }
}
