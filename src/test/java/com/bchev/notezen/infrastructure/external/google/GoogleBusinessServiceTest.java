package com.bchev.notezen.infrastructure.external.google;

import com.bchev.notezen.application.controller.dto.ListReviewsResponseDTO;
import com.bchev.notezen.application.controller.dto.ReviewDTO;
import com.bchev.notezen.application.controller.dto.ReviewerDTO;
import com.bchev.notezen.application.controller.dto.StarRatingDTO;
import com.bchev.notezen.domain.model.ReviewPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleBusinessServiceTest {

    private RestTemplate restTemplate;
    private GoogleBusinessService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        service = new GoogleBusinessService();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchAccountId_withAccounts_shouldReturnFirstAccountName() {
        Map<String, Object> body = Map.of("accounts", List.of(
                Map.of("name", "accounts/123"),
                Map.of("name", "accounts/456")
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        String accountId = service.fetchAccountId("token-abc");

        assertEquals("accounts/123", accountId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchAccountId_withNoAccounts_shouldReturnNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("accounts", List.of())));

        String accountId = service.fetchAccountId("token-abc");

        assertNull(accountId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchLocations_shouldReturnLocationsFromResponse() {
        Map<String, Object> body = Map.of("locations", List.of(
                Map.of("name", "accounts/123/locations/loc-1", "title", "Le Bistro")
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> locations = service.fetchLocations("accounts/123", "token-abc");

        assertEquals(1, locations.size());
        assertEquals("Le Bistro", locations.get(0).get("title"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchLocations_withNullLocations_shouldReturnEmptyList() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("otherKey", "value")));

        List<Map<String, Object>> locations = service.fetchLocations("accounts/123", "token-abc");

        assertTrue(locations.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchReviews_shouldBuildUrlWithPageSizeAndMapReviews() {
        ReviewDTO reviewDto = new ReviewDTO();
        reviewDto.setReviewId("review-1");
        reviewDto.setName("accounts/123/locations/loc-1/reviews/review-1");
        reviewDto.setComment("Super endroit");
        reviewDto.setStarRating(StarRatingDTO.FIVE);
        reviewDto.setCreateTime("2026-01-01T00:00:00Z");
        reviewDto.setReviewer(new ReviewerDTO("Alice", "photo-url", false));

        ListReviewsResponseDTO responseDto = new ListReviewsResponseDTO();
        ReflectionTestUtils.setField(responseDto, "reviews", List.of(reviewDto));
        ReflectionTestUtils.setField(responseDto, "nextPageToken", "next-page-token");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(restTemplate.exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(ListReviewsResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        ReviewPage page = service.fetchReviews("accounts/123", "loc-1", "token-abc", null);

        assertEquals(1, page.getReviews().size());
        assertEquals("review-1", page.getReviews().get(0).getReviewId());
        assertEquals("next-page-token", page.getNextPageToken());
        assertTrue(urlCaptor.getValue().contains("accounts/123/loc-1/reviews"));
        assertTrue(urlCaptor.getValue().contains("pageSize=50"));
        assertFalse(urlCaptor.getValue().contains("pageToken"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchReviews_withPageToken_shouldIncludePageTokenInUrl() {
        ListReviewsResponseDTO responseDto = new ListReviewsResponseDTO();
        ReflectionTestUtils.setField(responseDto, "reviews", List.of());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(restTemplate.exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(ListReviewsResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        service.fetchReviews("accounts/123", "loc-1", "token-abc", "page-2-token");

        assertTrue(urlCaptor.getValue().contains("pageToken=page-2-token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchReviews_withNullBody_shouldReturnEmptyReviewPage() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(ListReviewsResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(null));

        ReviewPage page = service.fetchReviews("accounts/123", "loc-1", "token-abc", null);

        assertTrue(page.getReviews().isEmpty());
        assertNull(page.getNextPageToken());
    }

    @Test
    void postReply_shouldPutReplyBodyToCorrectUrl() {
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(restTemplate.exchange(urlCaptor.capture(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.postReply("accounts/123", "loc-1", "review-1", "Merci !", "token-abc");

        assertTrue(urlCaptor.getValue().contains("accounts/123/loc-1/reviews/review-1/reply"));
    }

    @Test
    void postReply_onRestClientException_shouldWrapInRuntimeException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("network error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.postReply("accounts/123", "loc-1", "review-1", "Merci !", "token-abc"));

        assertEquals("Échec de la publication de la réponse Google", ex.getMessage());
    }
}
