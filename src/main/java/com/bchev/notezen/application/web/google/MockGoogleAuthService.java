package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@Profile("local")
public class MockGoogleAuthService implements GoogleAuthService {

    /**
     * Échange le code d'autorisation reçu contre un ensemble de tokens (access, refresh, id_token).
     */
    @Override
    public GoogleTokenResponseDTO exchangeCodeForTokens(String code) {
        GoogleTokenResponseDTO mockResponse = new GoogleTokenResponseDTO();
        mockResponse.setAccessToken("fake-access-token-" + UUID.randomUUID());
        mockResponse.setExpiresIn(3600);
        mockResponse.setIdToken("header.payload.signature");
        return mockResponse;
    }

    @Override
    public String getAuthorizationUrl() {
        return "";
    }

    /**
     * Rafraîchit un access_token expiré en utilisant le refresh_token stocké.
     */
    @Override
    public String refreshAccessToken(String refreshToken) {
        return "fake-access-token-" + UUID.randomUUID();
    }

    @Override
    public String extractEmailFromToken(String idToken) {
        log.info("[MOCK] Extraction de l'email pour le token : {}", idToken);
        return "dev@notezen.fr";
    }
}