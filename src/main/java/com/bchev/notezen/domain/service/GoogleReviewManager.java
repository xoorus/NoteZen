package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.GoogleAuthManager;
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
    public List<Review> getReviewsForUser(UserEntity user, String locationId, GoogleAuthManager googleAuthManager) {
        String validToken = googleAuthManager.getValidToken(user);
        return businessProvider.fetchReviews(user.getGoogleAccountId(), locationId, validToken);
    }

    /**
     * Logique de liaison de compte simplifiée pour le POC
     */
    public void linkAccount(String email, GoogleTokenResponseDTO tokens) {
        log.info("linking account");
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            return newUser;
        });
        log.info("user linked");

        // Utilisation du provider pour récupérer l'ID (sera mocké si profil mock actif)
        if (user.getGoogleAccountId() == null) {
            log.info("no accountID for this user, fetching...");
            String accountId = businessProvider.fetchAccountId(tokens.getAccessToken());
            log.info(">>>> MON GOOGLE ACCOUNT ID : {} <<<<", accountId);
            user.setGoogleAccountId(accountId);
        }

        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            log.info("setting refresh token");
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        userRepository.save(user);
    }

    public List<Map<String, Object>> getLocations(UserEntity user, GoogleAuthManager googleAuthManager) {
        String token = googleAuthManager.getValidToken(user);
        return businessProvider.fetchLocations(user.getGoogleAccountId(), token);
    }

    public void replyToReview(UserEntity user, String locationId, String reviewId, String text, GoogleAuthManager googleAuthManager) {
        String token = googleAuthManager.getValidToken(user);
        businessProvider.postReply(user.getGoogleAccountId(), locationId, reviewId, text, token);
    }

}