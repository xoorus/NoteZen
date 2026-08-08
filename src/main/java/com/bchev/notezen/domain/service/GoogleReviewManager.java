package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.ReviewPage;
import com.bchev.notezen.domain.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleReviewManager {

    private final BusinessProviderResolver businessProviderResolver;
    private final PlanFeatureResolver planFeatureResolver;

    public List<Review> getReviewsForUser(UserEntity user, String locationId, GoogleAuthManager googleAuthManager) {
        log.info("[GoogleReviewManager] Début récupération des avis pour le lieu: {}", locationId);

        String validToken = googleAuthManager.getValidToken(user);
        Instant thresholdDate = Instant.now().minus(14, ChronoUnit.DAYS);

        List<Review> allReviews = fetchAllReviewsWithinTimeframe(user, locationId, validToken, thresholdDate);

        return filterUnrepliedReviews(allReviews, thresholdDate);
    }

    public void replyToReview(UserEntity user, String locationId, String reviewId, String text, GoogleAuthManager googleAuthManager) {
        String token = googleAuthManager.getValidToken(user);
        log.info("[GoogleReviewManager] Envoi de la réponse vers l'API Google pour l'avis {}", reviewId);
        businessProviderResolver.resolve(user).postReply(user.getGoogleAccountId(), locationId, reviewId, text, token);
    }

    public List<Map<String, Object>> getLocations(UserEntity user, GoogleAuthManager googleAuthManager) {
        String token = googleAuthManager.getValidToken(user);
        log.info("[GoogleReviewManager] Appel API Google pour lister les lieux de l'utilisateur {}", user.getEmail());

        List<Map<String, Object>> allLocations =
            businessProviderResolver.resolve(user).fetchLocations(user.getGoogleAccountId(), token);

        return planFeatureResolver.filterLocationsByPlan(user, allLocations);
    }


    /**
     * Gère la boucle de pagination pour récupérer tous les avis récents.
     */
    private List<Review> fetchAllReviewsWithinTimeframe(UserEntity user, String locationId, String token, Instant thresholdDate) {
        List<Review> accumulatedReviews = new ArrayList<>();
        String nextPageToken = null;
        boolean shouldContinue = true;
        int pageCount = 0;

        while (shouldContinue) {
            pageCount++;
            log.info("[GoogleReviewManager] Récupération page {} (token: {})", pageCount, nextPageToken);

            ReviewPage page = businessProviderResolver.resolve(user).fetchReviews(user.getGoogleAccountId(), locationId, token, nextPageToken);

            if (page.getReviews() == null || page.getReviews().isEmpty()) {
                log.info("[GoogleReviewManager] Page vide reçue, arrêt de la récupération.");
                break;
            }

            accumulatedReviews.addAll(page.getReviews());
            nextPageToken = page.getNextPageToken();

            // On vérifie si le dernier avis de la page est déjà trop vieux
            shouldContinue = isLastReviewRecent(page.getReviews(), thresholdDate) && nextPageToken != null;
        }

        log.info("[GoogleReviewManager] Total collecté : {} avis sur {} pages", accumulatedReviews.size(), pageCount);
        return accumulatedReviews;
    }

    private boolean isLastReviewRecent(List<Review> reviews, Instant thresholdDate) {
        Review lastReview = reviews.get(reviews.size() - 1);
        Instant lastReviewDate = Instant.parse(lastReview.getCreateTime());
        boolean recent = lastReviewDate.isAfter(thresholdDate);
        if (!recent) log.info("[GoogleReviewManager] Dernier avis de la page hors délai (>14j). On stoppe la pagination.");
        return recent;
    }

    private List<Review> filterUnrepliedReviews(List<Review> reviews, Instant thresholdDate) {
        List<Review> recent = reviews.stream()
                .filter(r -> Instant.parse(r.getCreateTime()).isAfter(thresholdDate))
                .toList();

        List<Review> unreplied = recent.stream()
                .filter(r -> r.getReviewReply() == null)
                .toList();

        int tooOldCount = reviews.size() - recent.size();
        int alreadyRepliedCount = recent.size() - unreplied.size();

        log.info("[GoogleReviewManager] {} avis collectés au total, {} affichés ({} trop anciens, {} déjà répondus)",
                reviews.size(), unreplied.size(), tooOldCount, alreadyRepliedCount);

        return unreplied;
    }
}