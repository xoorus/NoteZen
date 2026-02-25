package com.bchev.notezen.infrastructure.external.google;

import com.bchev.notezen.application.controller.DTO.ListReviewsResponseDTO;
import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import com.bchev.notezen.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleReviewApi {

    private final RestTemplate restTemplate;

    /**
     * Appelle l'API Google pour récupérer les avis d'un établissement spécifique.
     * * @param accountId  L'ID du compte Google (ex: accounts/12345)
     * @param locationId L'ID de l'établissement (ex: locations/67890)
     * @param googleToken Un access_token valide (déjà rafraîchi si nécessaire)
     * @return Une liste d'objets Review formatée pour le cœur de l'appli NoteZen
     */
    public List<Review> getReviewsForUser(String accountId, String locationId, String googleToken) {
        // Construction de l'URL Google Business Profile API v4
        // Format : https://mybusiness.googleapis.com/v4/accounts/{accId}/locations/{locId}/reviews
        String url = String.format(
                "https://mybusiness.googleapis.com/v4/%s/%s/reviews",
                accountId, locationId
        );

        log.info("Appel Google Review API pour le compte {} et la location {}", accountId, locationId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // Récupération de la réponse via le DTO de liste
            ListReviewsResponseDTO responseDTO = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ListReviewsResponseDTO.class
            ).getBody();

            if (responseDTO == null || responseDTO.getReviews() == null) {
                return List.of();
            }

            // Transformation des DTOs techniques de Google en objets Review NoteZen
            return responseDTO.getReviews().stream()
                    .map(ReviewDTO::toReview)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à l'API Google Review : {}", e.getMessage());
            throw new RuntimeException("Impossible de récupérer les avis Google", e);
        }
    }
}