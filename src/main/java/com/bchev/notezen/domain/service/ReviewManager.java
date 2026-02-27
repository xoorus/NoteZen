package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewManager {

    private final UserRepository userRepository;
    private final GoogleReviewManager googleReviewManager;

    /**
     * Récupère la liste des avis pour un utilisateur interne NoteZen via son ID.
     * Gère automatiquement la récupération du compte et la validité du token.
     */
    public List<Review> getReviewsForUser(Long userId, String locationId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur NoteZen non trouvé (ID: " + userId + ")"));

        if (user.getGoogleAccountId() != null) {
            return googleReviewManager.getReviewsForUser(user, locationId);
        }

        return List.of();
    }

    public void replyToReview(Long userId, String locationId, String reviewId, String text) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getGoogleAccountId() != null) {
            googleReviewManager.replyToReview(user, locationId, reviewId, text);
        } else {
            throw new IllegalStateException("Aucun compte Google Business lié à cet utilisateur NoteZen.");
        }
    }

    public List<Map<String, Object>> getLocationsForUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getGoogleAccountId() == null) {
            return List.of();
        }

        return googleReviewManager.getLocations(user);
    }
}