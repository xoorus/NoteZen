package com.bchev.notezen.infrastructure.external.google;

import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;
import com.bchev.notezen.application.controller.DTO.ListReviewsResponseDTO;
import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.BusinessProvider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Profile("!local")
public class GoogleBusinessService implements BusinessProvider {
    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String fetchAccountId(String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/accounts";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.getBody().get("accounts");
        return (accounts != null && !accounts.isEmpty()) ? (String) accounts.get(0).get("name") : null;
    }

    @Override
    public List<Map<String, Object>> fetchLocations(String accountId, String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/" + accountId + "/locations?readMask=name,title";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        return (List<Map<String, Object>>) response.getBody().get("locations");
    }

    @Override
    public List<Review> fetchReviews(String accountId, String locationId, String accessToken) {
        // Attention : locationId doit être le nom complet "locations/123..."
        String url = String.format("https://mybusiness.googleapis.com/v4/%s/%s/reviews", accountId, locationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ListReviewsResponseDTO response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), ListReviewsResponseDTO.class).getBody();
        return response != null ? response.getReviews().stream().map(ReviewDTO::toReview).toList() : List.of();
    }

    @Override
    public String refreshAccessToken(String refreshToken) {

        log.info("Appel à l'API OAuth2 de Google pour rafraîchir le token...");

        String url = "https://oauth2.googleapis.com/token";

        // Préparation des paramètres pour la requête POST (Format x-www-form-urlencoded)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);         // Injecté via
        params.add("client_secret", clientSecret); // Injecté via ")
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // On réutilise votre DTO existant GoogleTokenResponseDTO
            ResponseEntity<GoogleTokenResponseDTO> response = restTemplate.postForEntity(
                    url,
                    request,
                    GoogleTokenResponseDTO.class
            );

            if (response.getBody() != null) {
                log.info("Nouveau token obtenu avec succès.");
                return response.getBody().getAccessToken();
            }
        } catch (Exception e) {
            log.error("Erreur lors du rafraîchissement du token Google : {}", e.getMessage());
            throw new RuntimeException("Impossible de rafraîchir l'accès Google", e);
        }

        return null;
    }

    @Override
    public void postReply(String accountId, String locationId, String reviewId, String text, String accessToken) {
        String url = String.format("https://mybusiness.googleapis.com/v4/%s/%s/reviews/%s/reply",
                accountId, locationId, reviewId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Corps de la requête attendu par Google
        Map<String, String> body = Map.of("comment", text);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Envoi d'une réponse à Google pour l'avis {}", reviewId);
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la réponse à Google : {}", e.getMessage());
            throw new RuntimeException("Échec de la publication de la réponse sur Google");
        }
    }
}