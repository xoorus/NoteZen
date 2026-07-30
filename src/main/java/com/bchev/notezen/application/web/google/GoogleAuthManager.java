package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.BusinessProviderResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthManager {

    private static final int TOKEN_EXPIRATION_MARGIN_MINUTES = 5;

    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;
    private final BusinessProviderResolver businessProviderResolver;

    /**
     * Point d'entrée principal pour lier un compte Google après le callback OAuth.
     * Ne vérifie pas l'autorisation métier (allowlist/abonnement) : un compte et un
     * JWT sont émis pour tout Google login réussi. L'accès aux fonctionnalités reste
     * gardé par AccessControlService, revérifié à chaque appel
     */
    public UserEntity linkAccount(String code) {
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);
        validateTokens(tokens);

        String email = extractEmailFromIdToken(tokens.getIdToken());
        UserEntity user = getOrCreateUserByEmail(email);

        updateGoogleAccountDetails(user, tokens);

        return userRepository.save(user);
    }

    /**
     * Fournit un token d'accès valide, en le rafraîchissant si nécessaire.
     */
    public String getValidToken(UserEntity user) {
        if (isTokenExpired(user)) {
            return refreshAndSaveToken(user);
        }
        return user.getGoogleAccessToken();
    }

    private void validateTokens(GoogleTokenResponseDTO tokens) {
        if (tokens == null || tokens.getIdToken() == null) {
            throw new RuntimeException("Échec de l'authentification Google : ID Token manquant.");
        }
    }

    private String extractEmailFromIdToken(String idToken) {
        try {
            var decoded = com.auth0.jwt.JWT.decode(idToken);
            String email = decoded.getClaim("email").asString();
            if (email == null || email.isEmpty()) {
                log.error("Email claim is null or empty in ID Token. Claims: {}", decoded.getClaims().keySet());
                throw new RuntimeException("Email not found in ID Token claims");
            }
            return email;
        } catch (Exception e) {
            log.error("Failed to extract email from ID Token: {}", e.getMessage());
            throw new RuntimeException("Impossible de lire l'email dans l'ID Token", e);
        }
    }

    private UserEntity getOrCreateUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Création d'un nouvel utilisateur NoteZen pour : {}", email);
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(email);
                    return newUser;
                });
    }

    private void updateGoogleAccountDetails(UserEntity user, GoogleTokenResponseDTO tokens) {
        // Mise à jour de l'Account ID (si pas encore présent)
        if (user.getGoogleAccountId() == null) {
            syncGoogleAccountId(user, tokens.getAccessToken());
        }

        // Mise à jour des tokens
        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }

        // Calcul de l'expiration
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));
    }

    private void syncGoogleAccountId(UserEntity user, String accessToken) {
        try {
            String accountId = businessProviderResolver.resolve(user).fetchAccountId(accessToken);
            user.setGoogleAccountId(accountId);
            log.info("Account ID synchronisé pour {}: {}", user.getEmail(), accountId);
        } catch (Exception e) {
            log.error("Échec de récupération de l'Account ID Google pour {}", user.getEmail());
            // Optionnel : ne pas bloquer si c'est juste un problème de quota
        }
    }

    private boolean isTokenExpired(UserEntity user) {
        if (user.getTokenExpiration() == null) return true;

        // On considère expiré s'il reste moins de 5 minutes
        return user.getTokenExpiration()
                .isBefore(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MARGIN_MINUTES));
    }

    private String refreshAndSaveToken(UserEntity user) {
        log.info("Le token Google pour {} est expiré. Tentative de rafraîchissement...", user.getEmail());

        if (user.getGoogleRefreshToken() == null) {
            throw new RuntimeException("Impossible de rafraîchir le token : Refresh Token absent.");
        }

        String newToken = googleAuthService.refreshAccessToken(user.getGoogleRefreshToken());

        user.setGoogleAccessToken(newToken);
        // On définit par défaut à 1h si l'API de refresh ne renvoie pas l'expiration
        user.setTokenExpiration(LocalDateTime.now().plusHours(1));

        userRepository.save(user);
        return newToken;
    }
}