package com.bchev.notezen.application.web.google;

import com.auth0.jwt.JWT;
import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("!mock")
public class RealGoogleAuthService implements GoogleAuthService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Échange le code d'autorisation reçu contre un ensemble de tokens (access, refresh, id_token).
     */
    @Override
    public GoogleTokenResponseDTO exchangeCodeForTokens(String code) {
        log.info("exchangeCodeForTokens real");
        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        log.info("request exchangeCodeForTokens : {}", params.toSingleValueMap());
        log.info("VÉRIFICATION FINALE - ClientID: [{}], RedirectURI: [{}]", clientId.trim(), redirectUri.trim());
        return restTemplate.postForObject(url, request, GoogleTokenResponseDTO.class);
    }

    @Override
    public String getAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/business.manage openid " +
                "https://www.googleapis.com/auth/userinfo.email " +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * Rafraîchit un access_token expiré en utilisant le refresh_token stocké.
     */
    @Override
    public String refreshAccessToken(String refreshToken) {
        log.info("Appel à l'API OAuth2 de Google pour rafraîchir le token...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        GoogleTokenResponseDTO response = restTemplate.postForObject(url, params, GoogleTokenResponseDTO.class);
        return response != null ? response.getAccessToken() : null;
    }

}