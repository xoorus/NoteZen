package com.bchev.notezen.application.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.application.web.google.GoogleAuthService;
import com.bchev.notezen.domain.service.BusinessProvider;
import com.bchev.notezen.domain.service.GoogleReviewManager;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.User;
import com.bchev.notezen.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);
        String email = extractEmailFromToken(tokens.getIdToken());
        googleReviewManager.linkAccount(email, tokens);
        return ResponseEntity.ok("Compte lié : " + email);
    }

   /* @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        String url = googleAuthService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));

    }*/
    @GetMapping("/auth-url")
    public void getGoogleAuthUrl(HttpServletResponse response) throws IOException {
        String url = googleAuthService.getAuthorizationUrl();
        response.sendRedirect(url);
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getLocations(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = authManager.getValidToken(user);
        return ResponseEntity.ok(businessProvider.fetchLocations(user.getGoogleAccountId(), token));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getReviews(@RequestParam String email, @RequestParam String locationId) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = authManager.getValidToken(user);
        List<Review> reviews = businessProvider.fetchReviews(user.getGoogleAccountId(), locationId, token);
        return ResponseEntity.ok(reviews);
    }

    private String extractEmailFromToken(String idToken) {
        DecodedJWT jwt = JWT.decode(idToken);
        return jwt.getClaim("email").asString();
    }
}