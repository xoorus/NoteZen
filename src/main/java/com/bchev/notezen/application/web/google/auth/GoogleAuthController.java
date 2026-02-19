package com.bchev.notezen.application.web.google.auth;

import com.bchev.notezen.core.google.GoogleReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    private final GoogleReviewService googleReviewService;

    public GoogleAuthController(GoogleAuthService googleAuthService, GoogleReviewService googleReviewService) {
        this.googleAuthService = googleAuthService;
        this.googleReviewService = googleReviewService;
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code, @RequestParam("email") String email) {
        // 1. Échanger le code contre les tokens
        GoogleTokenResponse tokens = googleAuthService.exchangeCodeForTokens(code);

        // 2. Appeler saveTokens pour enregistrer en base (H2/DB)
        googleReviewService.saveTokens(email, tokens);

        return ResponseEntity.ok("Authentification réussie et tokens sauvegardés !");
    }
}