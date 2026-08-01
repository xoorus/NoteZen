package com.bchev.notezen.application.web.google;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@Profile("mock")
public class MockGoogleAuthService implements GoogleAuthService {

    private static final String DEFAULT_MOCK_EMAIL = "dev@notezen.fr";

    /**
     * Échange le code d'autorisation reçu contre un ensemble de tokens (access, refresh, id_token).
     * Le "code" sert ici d'email à simuler (ex: appeler /api/google/callback?code=email@test.com) :
     * ça permet aux tests E2E de choisir quel utilisateur se connecte sans jamais appeler l'API
     * Google réelle. Un code qui ne ressemble pas à un email retombe sur un email par défaut.
     */
    @Override
    public GoogleTokenResponseDTO exchangeCodeForTokens(String code) {
        String email = (code != null && code.contains("@")) ? code : DEFAULT_MOCK_EMAIL;

        // JWT non signé (alg "none") : structurellement valide pour JWT.decode(), suffisant
        // pour du mock puisque personne ne vérifie sa signature côté extraction d'email.
        String idToken = JWT.create()
                .withClaim("email", email)
                .sign(Algorithm.none());

        GoogleTokenResponseDTO mockResponse = new GoogleTokenResponseDTO();
        mockResponse.setAccessToken("fake-access-token-" + UUID.randomUUID());
        mockResponse.setExpiresIn(3600);
        mockResponse.setIdToken(idToken);
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

}
