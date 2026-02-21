package com.bchev.notezen.application.web.google.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GoogleAuthService {

    @Value("${google.client-id}") private String clientId;
    @Value("${google.client-secret}") private String clientSecret;
    @Value("${google.redirect-uri}") private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Étape finale : Échanger le code reçu par le callback contre des tokens
     */
    public GoogleTokenResponseDTO exchangeCodeForTokens(String code) {
        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");
        // IMPORTANT pour avoir le refresh_token lors du premier login
        params.add("access_type", "offline");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        return restTemplate.postForObject(url, request, GoogleTokenResponseDTO.class);
    }

    public String getGoogleAccountId(String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/accounts";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.getBody().get("accounts");

            if (accounts != null && !accounts.isEmpty()) {
                // Retourne le nom du compte (format: "accounts/123456789")
                return (String) accounts.get(0).get("name");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du compte Google Business", e);
        }
        return null;
    }

    /**
     * Rafraîchir un access_token expiré sans intervention de l'utilisateur
     */
    public String refreshAccessToken(String refreshToken) {
        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        GoogleTokenResponseDTO response = restTemplate.postForObject(url, params, GoogleTokenResponseDTO.class);
        return response.getAccessToken();
    }
}
