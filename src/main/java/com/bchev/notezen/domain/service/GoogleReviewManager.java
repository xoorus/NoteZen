package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.User;
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
    public List<Review> getReviewsForUser(User user, String locationId) {
        // En mode Mock, le token n'a pas d'importance, mais on garde la logique
        String validToken = getValidToken(user);
            return businessProvider.fetchReviews(user.getGoogleAccountId(), locationId, validToken);
    }

    /**
     * Logique de liaison de compte simplifiée pour le POC
     */
    public void linkAccount(String email, GoogleTokenResponseDTO tokens) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            return newUser;
        });

        // Utilisation du provider pour récupérer l'ID (sera mocké si profil mock actif)
        if (user.getGoogleAccountId() == null) {
            String accountId = businessProvider.fetchAccountId(tokens.getAccessToken());
            user.setGoogleAccountId(accountId);
        }

        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        userRepository.save(user);
    }

    public List<Map<String, Object>> getLocations(User user) {
        String token = getValidToken(user); // Ta méthode qui gère le refresh auto
        return businessProvider.fetchLocations(user.getGoogleAccountId(), token);
    }

    public String getValidToken(User user) {
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