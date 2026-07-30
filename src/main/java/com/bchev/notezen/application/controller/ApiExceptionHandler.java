package com.bchev.notezen.application.controller;

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
 * Convertit systématiquement en 401 toute défaillance d'authentification/autorisation
 * (JWT absent, malformé, expiré, ou compte non autorisé) sur les endpoints /api/**.
 * Sans ce handler, ces exceptions remontent en 500 selon le point où elles sont levées,
 * ce qui empêche le frontend de distinguer "erreur serveur" de "session invalide" et
 * donc de rediriger proprement vers /unauthorized.
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
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Accès non autorisé, abonnement requis"));
    }
}
