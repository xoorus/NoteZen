package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.web.google.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
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
    private final GoogleBusinessClient googleClient;

    public String linkAccount(String code) {
        // 1. Échange du code contre les tokens (Access + Refresh + ID Token)
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);

        if (tokens == null || tokens.getIdToken() == null) {
            throw new RuntimeException("ID Token manquant. Vérifiez vos scopes (openid email).");
        }

        // 2. Extraction de l'email depuis le jeton ID
        String email = com.auth0.jwt.JWT.decode(tokens.getIdToken()).getClaim("email").asString();

        // 3. Récupération de l'utilisateur existant ou création d'un nouveau
        User user = userRepository.findByEmail(email).orElse(new User());
        user.setEmail(email);

        // 4. LOGIQUE OPTIMISÉE POUR L'ACCOUNT ID
        // On n'appelle Google Business que si on n'a pas déjà l'ID en base
        if (user.getGoogleAccountId() == null || user.getGoogleAccountId().isEmpty()) {
            try {
                log.info("Récupération de l'Account ID auprès de Google pour {}", email);
                String accountId = googleClient.fetchAccountId(tokens.getAccessToken());
                user.setGoogleAccountId(accountId);
            } catch (Exception e) {
                log.error("Impossible de récupérer l'Account ID (Quota 429 ?). Utilisation d'un ID temporaire.");
                // Optionnel : mettre un ID fictif pour ne pas bloquer le développement
                // user.setGoogleAccountId("accounts/temp-id-waiting-approval");
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

        userRepository.save(user);
        return email;
    }

    public String getValidToken(User user) {
        if (user.getTokenExpiration().isBefore(LocalDateTime.now().plusMinutes(5))) {
            String newToken = googleAuthService.refreshAccessToken(user.getGoogleRefreshToken());
            user.setGoogleAccessToken(newToken);
            user.setTokenExpiration(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            return newToken;
        }
        return user.getGoogleAccessToken();
    }
}