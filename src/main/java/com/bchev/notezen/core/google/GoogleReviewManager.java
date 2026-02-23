package com.bchev.notezen.core.google;

import com.bchev.notezen.application.web.google.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.GoogleAuthService;
import com.bchev.notezen.application.web.google.GoogleReviewApi;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleReviewManager {

    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;
    private final GoogleReviewApi googleReviewApi;

    /**
     * Récupère les avis en s'assurant que le token utilisé est toujours valide.
     */
    public List<Review> getReviewsForUser(User user, String locationId) {
        String validToken = getValidToken(user);
        return googleReviewApi.getReviewsForUser(user.getGoogleAccountId(), locationId, validToken);
    }

    /**
     * Sauvegarde ou met à jour les tokens et l'ID de compte Google.
     * Utilise 'tokenExpiration' pour l'uniformité.
     */
    public void saveTokens(String email, GoogleTokenResponseDTO tokens, String googleAccountId) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    return newUser;
                });

        user.setGoogleAccessToken(tokens.getAccessToken());

        // On ne met à jour le refresh_token que s'il est présent (envoyé uniquement au premier login)
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }

        user.setGoogleAccountId(googleAccountId);

        // Calcul de l'expiration basé sur le champ unique 'tokenExpiration'
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        userRepository.save(user);
        log.info("Tokens et compte Google sauvegardés pour l'utilisateur : {}", email);
    }

    /**
     * Vérifie la validité du token et le rafraîchit si nécessaire.
     * Utilise systématiquement 'tokenExpiration'.
     */
    public String getValidToken(User user) {
        // Seuil de sécurité de 5 minutes avant l'expiration réelle
        boolean needsRefresh = user.getTokenExpiration() == null ||
                LocalDateTime.now().isAfter(user.getTokenExpiration().minusMinutes(5));

        if (needsRefresh && user.getGoogleRefreshToken() != null) {
            log.info("Token expiré pour {}. Tentative de rafraîchissement...", user.getEmail());

            String newToken = googleAuthService.refreshAccessToken(user.getGoogleRefreshToken());
            user.setGoogleAccessToken(newToken);

            // On réinitialise l'expiration (standard Google : 3600s)
            user.setTokenExpiration(LocalDateTime.now().plusSeconds(3600));

            userRepository.save(user);
            return newToken;
        }

        return user.getGoogleAccessToken();
    }
}