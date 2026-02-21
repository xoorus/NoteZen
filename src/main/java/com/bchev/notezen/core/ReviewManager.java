package com.bchev.notezen.core;

import com.bchev.notezen.core.google.GoogleReviewManager;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "google.api.mode", havingValue = "real", matchIfMissing = true)
public class ReviewManager {

    UserRepository userRepository;
    GoogleReviewManager googleReviewService;

    public ReviewManager(UserRepository userRepository, GoogleReviewManager googleReviewService) {
        this.userRepository = userRepository;
        this.googleReviewService = googleReviewService;
    }

    public List<Review> getReviewsForUser(Long userId, String locationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Cette méthode gère maintenant toute la logique de rafraîchissement
        String validToken = googleReviewService.getValidToken(user);

        return googleReviewService.getReviewsForUser(user.getGoogleAccountId(), locationId, validToken);
    }

}
