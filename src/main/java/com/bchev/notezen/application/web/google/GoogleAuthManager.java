package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.web.google.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoogleAuthManager {
    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;
    private final GoogleBusinessClient googleClient;

    public String linkAccount(String code) {
        // 1. Échange tokens
        GoogleTokenResponseDTO tokens = googleAuthService.exchangeCodeForTokens(code);

        // 2. Extraction email
        String email = com.auth0.jwt.JWT.decode(tokens.getIdToken()).getClaim("email").asString();

        // 3. Récupération Account ID
        String accountId = googleClient.fetchAccountId(tokens.getAccessToken());

        // 4. Sauvegarde ou Mise à jour
        User user = userRepository.findByEmail(email).orElse(new User());
        user.setEmail(email);
        user.setGoogleAccessToken(tokens.getAccessToken());
        user.setGoogleRefreshToken(tokens.getRefreshToken());
        user.setGoogleAccountId(accountId);
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