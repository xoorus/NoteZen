package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.exception.UnauthorizedUserAccess;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewManager {

    private final UserRepository userRepository;
    private final GoogleReviewManager googleReviewManager;
    private final AccessControlService accessControlService;

    public List<Review> getReviewsForUser(Long userId, String locationId, GoogleAuthManager googleAuthManager) throws UnauthorizedUserAccess {
        log.info("[ReviewManager] Demande de récupération des avis pour l'utilisateur ID: {}", userId);

        UserEntity user = fetchUserOrThrow(userId);
        checkUserAuthorized(user);

        if (!hasGoogleAccountLinked(user)) {
            log.warn("[ReviewManager] Aucun compte Google lié pour l'utilisateur {}", userId);
            return List.of();
        }

        return googleReviewManager.getReviewsForUser(user, locationId, googleAuthManager);
    }

    public void replyToReview(Long userId, String locationId, String reviewId, String text, GoogleAuthManager googleAuthManager) throws UnauthorizedUserAccess {
        log.info("[ReviewManager] Tentative de réponse à l'avis {} par l'utilisateur {}", reviewId, userId);
        UserEntity user = fetchUserOrThrow(userId);
        checkUserAuthorized(user);

        ensureGoogleAccountIsLinked(user);

        googleReviewManager.replyToReview(user, locationId, reviewId, text, googleAuthManager);
        log.info("[ReviewManager] Réponse transmise avec succès au GoogleReviewManager");
    }

    public List<Map<String, Object>> getLocationsForUser(Long userId, GoogleAuthManager googleAuthManager) throws UnauthorizedUserAccess {
        log.info("[ReviewManager] Récupération des établissements pour l'utilisateur {}", userId);
        UserEntity user = fetchUserOrThrow(userId);
        checkUserAuthorized(user);

        if (!hasGoogleAccountLinked(user)) return List.of();

        return googleReviewManager.getLocations(user, googleAuthManager);
    }


    private UserEntity fetchUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[ReviewManager] Utilisateur ID {} non trouvé en base", userId);
                    return new RuntimeException("Utilisateur NoteZen non trouvé");
                });
    }

    private boolean hasGoogleAccountLinked(UserEntity user) {
        return user.getGoogleAccountId() != null;
    }

    private void ensureGoogleAccountIsLinked(UserEntity user) {
        if (!hasGoogleAccountLinked(user)) {
            log.error("[ReviewManager] Action impossible : l'utilisateur {} n'a pas de compte Google lié", user.getId());
            throw new IllegalStateException("Aucun compte Google Business lié.");
        }
    }

    private void checkUserAuthorized(UserEntity user) {
        if (!accessControlService.isAuthorized(user.getEmail())) {
            log.error("[Security] Accès API refusé pour l'utilisateur {} (Non autorisé)", user.getEmail());
            throw new UnauthorizedUserAccess("[Auth] Connexion bloquée :"+ user.getEmail()+" n'est pas dans la liste autorisée.", user.getEmail());
        }
    }
}