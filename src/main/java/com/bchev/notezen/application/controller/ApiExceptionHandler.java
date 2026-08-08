package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.exception.GoogleScopeMissingException;
import com.bchev.notezen.domain.exception.PaymentFailedException;
import com.bchev.notezen.domain.exception.SubscriptionCanceledException;
import com.bchev.notezen.domain.exception.UnauthorizedUserAccess;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Sépare deux causes distinctes d'échec sur les endpoints /api/** que le frontend
 * doit traiter différemment :
 * - 401 : JWT absent/malformé/expiré → problème d'authentification, une reconnexion
 *   Google silencieuse suffit à résoudre le problème.
 * - 403 : token valide mais accès refusé (pas d'abonnement, paiement échoué, compte
 *   non autorisé) → se reconnecter avec le même compte ne changerait rien, il faut
 *   montrer la page d'abonnement.
 * Sans ce handler, ces exceptions remontent en 500 selon le point où elles sont levées.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ JwtException.class, IllegalArgumentException.class })
    public ResponseEntity<?> handleInvalidToken(Exception e) {
        log.warn("[Security] Token invalide ou absent : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Session invalide, veuillez vous reconnecter"));
    }

    @ExceptionHandler({ UnauthorizedUserAccess.class, PaymentFailedException.class, SubscriptionCanceledException.class })
    public ResponseEntity<?> handleUnauthorizedAccess(RuntimeException e) {
        log.warn("[Security] Accès refusé : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Accès non autorisé, abonnement requis"));
    }

    /**
     * Cas distinct du 403 générique ci-dessus : token et abonnement valides, mais
     * l'utilisateur n'a pas coché la permission Google Business Profile au login.
     * Le frontend doit détecter "google_scope_missing" pour rediriger vers une page
     * dédiée plutôt que vers /unauthorized (qui parle d'abonnement, pas de permission Google).
     */
    @ExceptionHandler(GoogleScopeMissingException.class)
    public ResponseEntity<?> handleGoogleScopeMissing(GoogleScopeMissingException e) {
        log.warn("[Security] Permission Google Business Profile manquante : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "google_scope_missing"));
    }
}
