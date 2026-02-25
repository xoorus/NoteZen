package com.bchev.notezen.infrastructure.external.google;

import com.bchev.notezen.application.controller.DTO.ListReviewsResponseDTO;
import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GoogleBusinessService {
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchAccountId(String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/accounts";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.getBody().get("accounts");
        return (accounts != null && !accounts.isEmpty()) ? (String) accounts.get(0).get("name") : null;
    }

    public List<Map<String, Object>> fetchLocations(String accountId, String accessToken) {
        String url = "https://mybusinessbusinessinformation.googleapis.com/v1/" + accountId + "/locations?readMask=name,title";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        return (List<Map<String, Object>>) response.getBody().get("locations");
    }

    public List<ReviewDTO> fetchReviews(String accountId, String locationId, String accessToken) {
        // Attention : locationId doit être le nom complet "locations/123..."
        String url = String.format("https://mybusiness.googleapis.com/v4/%s/%s/reviews", accountId, locationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ListReviewsResponseDTO response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), ListReviewsResponseDTO.class).getBody();
        return response != null ? response.getReviews() : List.of();
    }
}