package com.bchev.notezen.infrastructure.external.mock;

import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import com.bchev.notezen.application.controller.DTO.ReviewerDTO;
import com.bchev.notezen.application.controller.DTO.StarRatingDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.BusinessProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("local")
@Slf4j
public class MockBusinessService implements BusinessProvider {

    @Override
    public String fetchAccountId(String accessToken) {
        return "accounts/mock-user-123";
    }

    @Override
    public List<Map<String, Object>> fetchLocations(String accountId, String accessToken) {
        return List.of(
                Map.of("name", accountId + "/locations/loc-paris", "title", "Le Petit Bistro - Paris"),
                Map.of("name", accountId + "/locations/loc-lyon", "title", "Le Petit Bistro - Lyon")
        );
    }

    @Override
    public List<Review> fetchReviews(String accountId, String locationId, String accessToken) {
        List<ReviewDTO> reviewsDTO = new ArrayList<>();
        if (locationId.contains("loc-paris")) {
            reviewsDTO.add(createReview("1", "Alice L.", "Excellent service, je recommande !", "FIVE"));
            reviewsDTO.add(createReview("2", "Marc A.", "Un peu bruyant mais très bon.", "FOUR"));
        }
        reviewsDTO.add(createReview("3", "Thomas P.", "Déçu par l'accueil.", "TWO"));
        reviewsDTO.add(createReview("4", "Jean Dupont", "Excellent accueil, je reviendrai !", "FIVE"));
        reviewsDTO.add(createReview("5", "Alice Martin", "Trop d'attente pour être servi...", "TWO"));

        return reviewsDTO.stream().map(ReviewDTO::toReview).toList();
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        return "new-mock-access-token-" + System.currentTimeMillis();
    }

    private ReviewDTO createReview(String id, String author, String comment, String rating) {
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(id);
        dto.setReviewer(new ReviewerDTO(author, "", false));
        dto.setComment(comment);
        dto.setStarRating(StarRatingDTO.valueOf(rating));
        dto.setCreateTime(java.time.Instant.now().toString());
        return dto;
    }
}