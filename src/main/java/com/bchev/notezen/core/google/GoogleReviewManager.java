package com.bchev.notezen.core.google;

import com.bchev.notezen.application.web.google.auth.GoogleAuthService;
import com.bchev.notezen.application.web.google.auth.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.review.GoogleReviewApi;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoogleReviewManager {

    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;
    private final GoogleReviewApi googleReviewApi;

    public GoogleReviewManager(UserRepository userRepository, GoogleAuthService googleAuthService, GoogleReviewApi googleReviewApi) {
        this.userRepository = userRepository;
        this.googleAuthService = googleAuthService;
        this.googleReviewApi = googleReviewApi;
    }

    public List<Review> getReviewsForUser(String accountId, String locationId, String googleToken) {
        return googleReviewApi.getReviewsForUser(accountId, locationId, googleToken);
    }

    public void saveTokens(String email, GoogleTokenResponseDTO tokens, String googleAccountId) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    return newUser;
                });

        user.setGoogleAccessToken(tokens.getAccessToken());
        user.setGoogleRefreshToken(tokens.getRefreshToken());
        user.setGoogleAccountId(googleAccountId); // TRÈS IMPORTANT

        // Calcule la date d'expiration
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        userRepository.save(user);
    }

    public String getValidToken(User user) {
        // Vérifie si le token expire dans moins de 5 minutes pour anticiper
        boolean isExpired = user.getGoogleTokenExpiresAt() == null ||
                LocalDateTime.now().isAfter(user.getGoogleTokenExpiresAt().minusMinutes(5));

        if (isExpired && user.getGoogleRefreshToken() != null) {
            // Appeler Google pour un nouveau access_token
            String newToken = googleAuthService.refreshAccessToken(user.getGoogleRefreshToken());

            user.setGoogleAccessToken(newToken);
            // On remet à jour l'expiration (Google renvoie généralement 3600s)
            user.setGoogleTokenExpiresAt(LocalDateTime.now().plusSeconds(3600));

            userRepository.save(user);
            return newToken;
        }

        return user.getGoogleAccessToken();
    }

}