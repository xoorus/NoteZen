package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.application.web.google.GoogleAuthService;
import com.bchev.notezen.domain.exception.PaymentFailedException;
import com.bchev.notezen.domain.exception.SubscriptionCanceledException;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.service.AccessControlService;
import com.bchev.notezen.domain.service.SubscriptionManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/google")
@CrossOrigin(origins = { "http://localhost:4200", "https://www.notezen.fr", "https://notezen.fr" })
@RequiredArgsConstructor
public class GoogleBusinessController {

    private final GoogleAuthManager googleAuthManager;
    private final GoogleAuthService googleAuthService;
    private final AccessControlService accessControlService;
    private final SubscriptionManager subscriptionManager;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${front.url}")
    private String frontUrl;

    /**
     * Après un login Google réussi : compte existant/autorisé (allowlist ou
     * abonnement actif) → dashboard, comme un login classique. Sinon → Checkout
     * Stripe directement, sans page intermédiaire
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {

        log.info("callback");
        UserEntity user;
        try {
            user = googleAuthManager.linkAccount(code);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
            response.sendRedirect(frontUrl + "login?error=auth_failed");
            return;
        }

        String noteZenToken = TokenUtils.generateToken(user.getId());

        if (isAuthorized(user.getEmail())) {
            response.sendRedirect(frontUrl + "dashboard?token=" + noteZenToken);
            return;
        }

        try {
            // Succès et annulation renvoient tous les deux vers le dashboard : s'il
            // n'est pas encore autorisé (paiement annulé, webhook pas encore arrivé),
            // le 401 sur les appels API l'y renverra automatiquement vers /unauthorized.
            String checkoutUrl = subscriptionManager.startCheckout(
                    user,
                    frontUrl + "dashboard?token=" + noteZenToken,
                    frontUrl + "dashboard?token=" + noteZenToken
            );
            response.sendRedirect(checkoutUrl);
        } catch (Exception e) {
            log.error("Impossible de démarrer le checkout Stripe pour {}: {}", user.getEmail(), e.getMessage());
            response.sendRedirect(frontUrl + "unauthorized?token=" + noteZenToken + "&email=" + user.getEmail());
        }
    }

    /**
     * Un abonnement en échec de paiement ou annulé compte comme non autorisé
     * pour la redirection post-login : l'utilisateur reçoit quand même son JWT
     * pour pouvoir relancer un paiement depuis la page /unauthorized.
     */
    private boolean isAuthorized(String email) {
        try {
            return accessControlService.isAuthorized(email);
        } catch (PaymentFailedException | SubscriptionCanceledException e) {
            return false;
        }
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        if (isLocalProfileActive()) {
            // En local, on renvoie une URL qui pointe directement vers notre callback
            // avec un faux code, pour simuler la fin du processus
            String mockCallbackUrl = "http://localhost:8080/api/google/callback?code=mock-code";
            return ResponseEntity.ok(Map.of("url", mockCallbackUrl));
        }
        return ResponseEntity.ok(Map.of("url", googleAuthService.getAuthorizationUrl()));
    }

    private boolean isLocalProfileActive() {
        return "local".equals(activeProfile);
    }

}