package com.bchev.notezen.application.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.application.web.google.GoogleAuthService;
import com.bchev.notezen.domain.service.BusinessProvider;
import com.bchev.notezen.domain.service.GoogleReviewManager;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class GoogleBusinessController {

    private final GoogleAuthManager authManager;
    private final BusinessProvider businessProvider;
    private final UserRepository userRepository;
    private final GoogleReviewManager googleReviewManager;
    private final GoogleAuthService googleAuthService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);
        String email = extractEmailFromToken(tokens.getIdToken());
        googleReviewManager.linkAccount(email, tokens);
        return ResponseEntity.ok("Compte lié : " + email);
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        if (isLocalProfileActive()) {
            // En local, on renvoie une URL qui pointe directement vers notre callback
            // avec un faux code, pour simuler la fin du processus
            String mockCallbackUrl = "http://localhost:8080/api/google/callback?code=mock-code";
            return ResponseEntity.ok(Map.of("url", mockCallbackUrl));
        }
        return ResponseEntity.ok(Map.of("url", googleAuthService.getAuthorizationUrl()));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getLocations(@RequestParam String email) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        String token = authManager.getValidToken(user);
        return ResponseEntity.ok(businessProvider.fetchLocations(user.getGoogleAccountId(), token));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getReviews(@RequestParam String email, @RequestParam String locationId) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        String token = authManager.getValidToken(user);
        List<Review> reviews = businessProvider.fetchReviews(user.getGoogleAccountId(), locationId, token);
        return ResponseEntity.ok(reviews);
    }

    private String extractEmailFromToken(String idToken) {
        DecodedJWT jwt = JWT.decode(idToken);
        return jwt.getClaim("email").asString();
    }

    private boolean isLocalProfileActive() {
        return "local".equals(activeProfile);
    }
}