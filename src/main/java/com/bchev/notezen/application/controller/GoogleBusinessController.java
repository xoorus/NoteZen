package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.application.web.google.GoogleAuthService;
import com.bchev.notezen.domain.service.BusinessProvider;
import com.bchev.notezen.domain.service.GoogleReviewManager;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/google")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class GoogleBusinessController {

    private final GoogleReviewManager googleReviewManager;
    private final GoogleAuthService googleAuthService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) throws IOException {

        log.info("callback");
        try {
            GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);
            String email = googleAuthService.extractEmailFromToken(tokens.getIdToken());
            log.info("email : {}", email);
            googleReviewManager.linkAccount(email, tokens);
            String frontendUrl = "http://localhost:4200/dashboard?token=" + tokens.getIdToken(); // Utilise l'ID Token comme JWT simple
            response.sendRedirect(frontendUrl);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
        }
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
/*
    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getLocations(
            @RequestHeader("Authorization") String authHeader) { // On utilise le Header, pas le Param

        log.info("GoogleBusinessController getLocations API");
        String email;
        String jwt = authHeader.replace("Bearer ", "");

        if (isLocalProfileActive() && "header.payload.signature".equals(jwt)) {
            email = "dev@notezen.fr";
        } else {
            try {
                email = com.auth0.jwt.JWT.decode(jwt).getClaim("email").asString();
            } catch (Exception e) {
                return ResponseEntity.status(401).build(); // Token mal formé
            }
        }

        // 2. Recherche de l'utilisateur avec l'email extrait
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 3. Récupération d'un token Google valide (Refresh si besoin)
        String token = authManager.getValidToken(user);

        return ResponseEntity.ok(businessProvider.fetchLocations(user.getGoogleAccountId(), token));
    }*/
/*
    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getReviews(
            @RequestHeader("Authorization") String authHeader, // Idem ici
            @RequestParam String locationId) {
        log.info("GoogleBusinessController Authorization API");

        String email;
        String jwt = authHeader.replace("Bearer ", "");

        if (isLocalProfileActive() && "header.payload.signature".equals(jwt)) {
            email = "dev@notezen.fr";
        } else {
            try {
                email = com.auth0.jwt.JWT.decode(jwt).getClaim("email").asString();
            } catch (Exception e) {
                return ResponseEntity.status(401).build(); // Token mal formé
            }
        }

        // 2. Recherche de l'utilisateur avec l'email extrait
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 3. Récupération d'un token Google valide (Refresh si besoin)
        String token = authManager.getValidToken(user);

        List<Review> reviews = googleReviewManager.fetchReviews(user.getGoogleAccountId(), locationId, token);
        return ResponseEntity.ok(reviews);
    }*/

    private boolean isLocalProfileActive() {
        return "local".equals(activeProfile);
    }

}