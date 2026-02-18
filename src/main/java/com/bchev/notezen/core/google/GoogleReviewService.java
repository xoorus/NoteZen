package com.bchev.notezen.core.google;

import com.bchev.notezen.application.web.google.auth.GoogleAuthService;
import com.bchev.notezen.application.web.google.auth.GoogleTokenResponse;
import com.bchev.notezen.application.web.google.review.GoogleReviewApi;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleReviewService {

    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;
    private final GoogleReviewApi googleReviewApi;

    public GoogleReviewService(UserRepository userRepository, GoogleAuthService googleAuthService, GoogleReviewApi googleReviewApi) {
        this.userRepository = userRepository;
        this.googleAuthService = googleAuthService;
        this.googleReviewApi = googleReviewApi;
    }

    public List<Review> getReviewsForUser(String accountId, String locationId, String googleToken) {
        return googleReviewApi.getReviewsForUser(accountId, locationId, googleToken);
    }

    public void saveTokens(String email, GoogleTokenResponse tokens) {
        User user = userRepository.findByEmail(email).orElse(new User());
        user.setEmail(email);
        user.setGoogleAccessToken(tokens.getAccessToken());

        // Google n'envoie le Refresh Token que la TOUTE première fois
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }

        userRepository.save(user); // Enregistre en RAM (H2)
    }

    public String getValidToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non authentifié avec Google"));

        // Logique de rafraîchissement si nécessaire...
        return user.getGoogleAccessToken();
    }
}