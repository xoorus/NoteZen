package com.bchev.notezen.infrastructure.external.google;

import com.bchev.notezen.application.controller.dto.ListReviewsResponseDTO;
import com.bchev.notezen.application.controller.dto.ReviewDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.ReviewPage;
import com.bchev.notezen.domain.service.BusinessProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@Qualifier("realBusinessProvider")
@Slf4j
@Profile("!local")
public class GoogleBusinessService implements BusinessProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BUSINESS_INFO_BASE_URL = "https://mybusinessbusinessinformation.googleapis.com/v1";
    private static final String MY_BUSINESS_V4_BASE_URL = "https://mybusiness.googleapis.com/v4";

    @Override
    public String fetchAccountId(String accessToken) {
        log.info("[GoogleBusinessService] Tentative de récupération de l'Account ID Google");
        String url = BUSINESS_INFO_BASE_URL + "/accounts";

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, createHttpEntity(accessToken), Map.class);

        List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.getBody().get("accounts");

        if (accounts == null || accounts.isEmpty()) {
            log.warn("[GoogleBusinessService] Aucun compte Google Business trouvé pour ce token.");
            return null;
        }

        String accountId = (String) accounts.get(0).get("name");
        log.info("[GoogleBusinessService] Account ID récupéré : {}", accountId);
        return accountId;
    }

    @Override
    public ReviewPage fetchReviews(String accountId, String locationId, String accessToken, String pageToken) {
        log.info("[GoogleBusinessService] Récupération des avis pour location: {} (PageToken: {})", locationId, pageToken);

        String url = buildReviewUrl(accountId, locationId, pageToken);

        ResponseEntity<ListReviewsResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, createHttpEntity(accessToken), ListReviewsResponseDTO.class);

        ListReviewsResponseDTO body = response.getBody();
        return mapToReviewPage(body);
    }

    @Override
    public void postReply(String accountId, String locationId, String reviewId, String text, String accessToken) {
        log.info("[GoogleBusinessService] Envoi d'une réponse à l'avis {} sur le lieu {}", reviewId, locationId);

        String url = String.format("%s/%s/%s/reviews/%s/reply", MY_BUSINESS_V4_BASE_URL, accountId, locationId, reviewId);

        Map<String, String> body = Map.of("comment", text);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, createHeaders(accessToken));

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("[GoogleBusinessService] Réponse publiée avec succès sur Google.");
        } catch (Exception e) {
            log.error("[GoogleBusinessService] Erreur lors de la publication de la réponse : {}", e.getMessage());
            throw new RuntimeException("Échec de la publication de la réponse Google", e);
        }
    }

    @Override
    public List<Map<String, Object>> fetchLocations(String accountId, String accessToken) {
        log.info("[GoogleBusinessService] Récupération des établissements pour le compte: {}", accountId);
        String url = String.format("%s/%s/locations?readMask=name,title,storeCode", BUSINESS_INFO_BASE_URL, accountId);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, createHttpEntity(accessToken), Map.class);

        List<Map<String, Object>> locations = (List<Map<String, Object>>) response.getBody().get("locations");
        log.info("[GoogleBusinessService] {} lieux récupérés.", locations != null ? locations.size() : 0);
        return locations != null ? locations : List.of();
    }

    private String buildReviewUrl(String accountId, String locationId, String pageToken) {
        log.debug("[GoogleBusinessService] Construction de l'URL pour les avis...");

        // On construit l'URL de base sans concaténation manuelle risquée
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(MY_BUSINESS_V4_BASE_URL)
                .pathSegment(accountId, locationId, "reviews")
                .queryParam("pageSize", 50);

        if (pageToken != null && !pageToken.isEmpty()) {
            builder.queryParam("pageToken", pageToken);
        }

        String finalUrl = builder.build().toUriString();
        log.info("[GoogleBusinessService] URL finale construite : {}", finalUrl);
        return finalUrl;
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpEntity<Void> createHttpEntity(String accessToken) {
        return new HttpEntity<>(createHeaders(accessToken));
    }

    private ReviewPage mapToReviewPage(ListReviewsResponseDTO body) {
        if (body == null || body.getReviews() == null) {
            return new ReviewPage(List.of(), null);
        }

        List<Review> reviews = body.getReviews().stream()
                .map(ReviewDTO::toReview)
                .toList();

        log.info("[GoogleBusinessService] Mapping terminé : {} avis convertis.", reviews.size());
        return new ReviewPage(reviews, body.getNextPageToken());
    }
}