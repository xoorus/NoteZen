package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.ReviewPage;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleReviewManager {

    private final UserRepository userRepository;
    private final BusinessProvider businessProvider;

    public List<Review> getReviewsForUser(UserEntity user, String locationId, GoogleAuthManager googleAuthManager) {
        String validToken = googleAuthManager.getValidToken(user);
        Instant twoWeeksAgo = Instant.now().minus(14, ChronoUnit.DAYS);

        List<Review> allRelevantReviews = new ArrayList<>();
        String nextPageToken = null;
        boolean keepFetching = true;

        log.info("Début de la récupération exhaustive des avis pour les 14 derniers jours");

        while (keepFetching) {
            // 1. Appel d'une page (50 avis max)
            ReviewPage page = businessProvider.fetchReviews(
                    user.getGoogleAccountId(),
                    locationId,
                    validToken,
                    nextPageToken
            );

            if (page.getReviews() == null || page.getReviews().isEmpty()) {
                break;
            }

            // 2. Filtrage immédiat de la page pour voir si on continue
            List<Review> pageReviews = page.getReviews();
            allRelevantReviews.addAll(pageReviews);

            // 3. On regarde le dernier avis de la page reçue
            Review lastReviewOfPage = pageReviews.get(pageReviews.size() - 1);
            Instant lastReviewDate = Instant.parse(lastReviewOfPage.getCreateTime());

            // 4. Condition d'arrêt :
            // Si le dernier avis de la page est déjà plus vieux que 14 jours,
            // pas besoin de charger la page suivante.
            nextPageToken = page.getNextPageToken();
            if (nextPageToken == null || lastReviewDate.isBefore(twoWeeksAgo)) {
                keepFetching = false;
            }
        }

        // Filtre final de précision (pour éliminer les quelques avis trop vieux de la dernière page)
        return allRelevantReviews.stream()
                .filter(review -> Instant.parse(review.getCreateTime()).isAfter(twoWeeksAgo))
                .filter(review -> review.getReviewReply() == null)
                .toList();
    }

    /**
     * Logique de liaison de compte simplifiée pour le POC
     */
    public void linkAccount(String email, GoogleTokenResponseDTO tokens) {
        log.info("linking account");
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            return newUser;
        });
        log.info("user linked");

        // Utilisation du provider pour récupérer l'ID (sera mocké si profil mock actif)
        if (user.getGoogleAccountId() == null) {
            log.info("no accountID for this user, fetching...");
            String accountId = businessProvider.fetchAccountId(tokens.getAccessToken());
            log.info(">>>> MON GOOGLE ACCOUNT ID : {} <<<<", accountId);
            user.setGoogleAccountId(accountId);
        }

        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            log.info("setting refresh token");
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        user.setTokenExpiration(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));

        userRepository.save(user);
    }

    public List<Map<String, Object>> getLocations(UserEntity user, GoogleAuthManager googleAuthManager) {
        String token = googleAuthManager.getValidToken(user);
        return businessProvider.fetchLocations(user.getGoogleAccountId(), token);
    }

    public void replyToReview(UserEntity user, String locationId, String reviewId, String text, GoogleAuthManager googleAuthManager) {
        String token = googleAuthManager.getValidToken(user);
        businessProvider.postReply(user.getGoogleAccountId(), locationId, reviewId, text, token);
    }

}