package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.BusinessProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthManager {
    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;
    private final BusinessProvider businessProvider;


    public UserEntity linkAccount(String code) {
        // 1. Échange du code contre les tokens (Access + Refresh + ID Token)
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);

        if (tokens == null || tokens.getIdToken() == null) {
            throw new RuntimeException("ID Token manquant. Vérifiez vos scopes (openid email).");
        }

        // 2. Extraction de l'email depuis le jeton ID
        String email = com.auth0.jwt.JWT.decode(tokens.getIdToken()).getClaim("email").asString();

        // 3. Récupération de l'utilisateur existant ou création d'un nouveau
        UserEntity user = userRepository.findByEmail(email).orElse(new UserEntity());
        user.setEmail(email);

        // 4. LOGIQUE OPTIMISÉE POUR L'ACCOUNT ID On appelle Google Business que si on n'a pas déjà l'ID en base
        if (user.getGoogleAccountId() == null || user.getGoogleAccountId().isEmpty()) {
            try {
                log.info("Récupération de l'Account ID auprès de Google pour {}", email);
                String accountId = businessProvider.fetchAccountId(tokens.getAccessToken());
                log.info(">>>> MON GOOGLE ACCOUNT ID : {} <<<<", accountId);
                user.setGoogleAccountId(accountId);
            } catch (Exception e) {
                log.error("Impossible de récupérer l'Account ID (Quota 429 ?). Utilisation d'un ID temporaire.");
            }
        } else {
            log.info("L'Account ID pour {} est déjà connu en base : {}", email, user.getGoogleAccountId());
        }

        // 5. Mise à jour des jetons et de l'expiration
        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        return userRepository.save(user);
    }

    public String getValidToken(UserEntity user) {
        // Vérification de l'expiration (ex: 5 minutes de marge)
        if (user.getTokenExpiration() == null ||
                user.getTokenExpiration().isBefore(LocalDateTime.now().plusMinutes(5))) {

            log.info("Rafraîchissement du token pour : {}", user.getEmail());
            String newToken = googleAuthService.refreshAccessToken(user.getGoogleRefreshToken());

            user.setGoogleAccessToken(newToken);
            // Google renvoie généralement une validité de 3600s
            user.setTokenExpiration(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            return newToken;
        }
        return user.getGoogleAccessToken();
    }

}