package com.bchev.notezen.core;

import com.bchev.notezen.core.google.GoogleReviewManager;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "google.api.mode", havingValue = "real", matchIfMissing = true)
public class ReviewManager {

    private final UserRepository userRepository;
    private final GoogleReviewManager googleReviewService;

    /**
     * Récupère la liste des avis pour un utilisateur interne NoteZen via son ID.
     * Gère automatiquement la récupération du compte et la validité du token.
     */
    public List<Review> getReviewsForUser(Long userId, String locationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur NoteZen non trouvé (ID: " + userId + ")"));

        // Délégation de la logique de token et d'API au manager spécialisé
        return googleReviewService.getReviewsForUser(user, locationId);
    }
}