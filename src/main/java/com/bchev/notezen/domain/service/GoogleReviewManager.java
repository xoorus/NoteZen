package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleReviewManager {

    private final UserRepository userRepository;
    private final BusinessProvider businessProvider;

    /**
     * Récupère les avis via le provider (Mock ou Réel).
     */
    public List<Review> getReviewsForUser(UserEntity user, String locationId) {
        String validToken = getValidToken(user);
        return businessProvider.fetchReviews(user.getGoogleAccountId(), locationId, validToken);
    }

    /**
     * Logique de liaison de compte simplifiée pour le POC
     */
    public void linkAccount(String email, GoogleTokenResponseDTO tokens) {
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            return newUser;
        });

        // Utilisation du provider pour récupérer l'ID (sera mocké si profil mock actif)
        if (user.getGoogleAccountId() == null) {
            String accountId = businessProvider.fetchAccountId(tokens.getAccessToken());
            log.info(">>>> MON GOOGLE ACCOUNT ID : {} <<<<", accountId);
            user.setGoogleAccountId(accountId);
        }

        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        userRepository.save(user);
    }

    public List<Map<String, Object>> getLocations(UserEntity user) {
        String token = getValidToken(user);
        return businessProvider.fetchLocations(user.getGoogleAccountId(), token);
    }

    public void replyToReview(UserEntity user, String locationId, String reviewId, String text) {
        String token = getValidToken(user);
        businessProvider.postReply(user.getGoogleAccountId(), locationId, reviewId, text, token);
    }

    public String getValidToken(UserEntity user) {
        if (user.getGoogleRefreshToken() == null && "mock-acc".equals(user.getGoogleAccountId())) {
            return "mock-token";
        }

        boolean needsRefresh = user.getTokenExpiration() == null ||
                LocalDateTime.now().isAfter(user.getTokenExpiration().minusMinutes(5));

        if (needsRefresh && user.getGoogleRefreshToken() != null) {
            log.info("Token expiré pour {}. Rafraîchissement via provider...", user.getEmail());
            // Le provider peut aussi gérer le refresh
            String newToken = businessProvider.refreshAccessToken(user.getGoogleRefreshToken());
            user.setGoogleAccessToken(newToken);
            user.setTokenExpiration(LocalDateTime.now().plusSeconds(3600));
            userRepository.save(user);
            return newToken;
        }
        return user.getGoogleAccessToken();
    }
}