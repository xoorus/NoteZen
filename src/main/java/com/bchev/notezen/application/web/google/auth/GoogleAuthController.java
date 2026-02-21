package com.bchev.notezen.application.web.google.auth;

import com.bchev.notezen.core.google.GoogleReviewManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    private final GoogleReviewManager googleReviewService;

    public GoogleAuthController(GoogleAuthService googleAuthService, GoogleReviewManager googleReviewService) {
        this.googleAuthService = googleAuthService;
        this.googleReviewService = googleReviewService;
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) {
        // 1. On récupère les tokens
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);

        // 2. On extrait l'email de l'id_token (JWT)
        String idToken = tokens.getIdToken();
        String email = com.auth0.jwt.JWT.decode(idToken).getClaim("email").asString();

        // 3. On récupère l'ID de compte Google Business
        String googleAccountId = googleAuthService.getGoogleAccountId(tokens.getAccessToken());

        // 4. On enregistre tout en base
        // Modifie ta méthode saveTokens pour accepter aussi le googleAccountId
        googleReviewService.saveTokens(email, tokens, googleAccountId);

        return ResponseEntity.ok("Bravo ! Ton compte " + email + " est lié. Account ID: " + googleAccountId);
    }
}