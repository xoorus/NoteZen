package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
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
     * Échange le code d'autorisation reçu contre un ensemble de tokens (access, refresh, id_token).
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
        // Nécessaire pour obtenir le refresh_token lors de la première connexion
        params.add("access_type", "offline");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        return restTemplate.postForObject(url, request, GoogleTokenResponseDTO.class);
    }

    public String getAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/business.manage openid email" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * Récupère l'ID du compte Google Business (format "accounts/123...").
     */
    public String getGoogleAccountId(String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/accounts";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.getBody().get("accounts");

            if (accounts != null && !accounts.isEmpty()) {
                return (String) accounts.get(0).get("name");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du compte Google Business", e);
        }
        return null;
    }

    /**
     * Rafraîchit un access_token expiré en utilisant le refresh_token stocké.
     */
    public String refreshAccessToken(String refreshToken) {
        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        GoogleTokenResponseDTO response = restTemplate.postForObject(url, params, GoogleTokenResponseDTO.class);
        return response != null ? response.getAccessToken() : null;
    }

    /**
     * Récupère la liste des établissements (locations) associés à un compte.
     */
    public List<Map<String, Object>> getGoogleLocations(String accountId, String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/" + accountId + "/locations?readMask=name,title,storeCode";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return (List<Map<String, Object>>) response.getBody().get("locations");
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des établissements", e);
            return List.of();
        }
    }
}