package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.exception.UnauthorizedUserAccess;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.ReviewManager;
import com.bchev.notezen.domain.helpers.TokenUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteZenReviewControllerTest {

    @Mock
    private ReviewManager reviewManager;

    @Mock
    private GoogleAuthManager googleAuthManager;

    @Mock
    private HttpServletResponse response;

    private noteZenReviewController controller;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-1234567890ab";

    @BeforeEach
    void setUp() {
        controller = new noteZenReviewController(reviewManager, googleAuthManager);
        ReflectionTestUtils.setField(TokenUtils.class, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(controller, "appVersion", "1.0.0");
    }

    @Test
    void getVersion_shouldReturnAppVersion() {
        // When
        ResponseEntity<String> response = controller.getVersion();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("1.0.0", response.getBody());
    }

    @Test
    void getReviews_withValidJwt_shouldReturnReviews() throws IOException, UnauthorizedUserAccess {
        // Given
        String jwt = "Bearer " + generateValidToken(1L);
        String locationId = "location-123";

        when(reviewManager.getReviewsForUser(1L, locationId, googleAuthManager))
                .thenReturn(List.of());

        // When
        ResponseEntity<List<Review>> response = controller.getReviews(jwt, locationId, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reviewManager).getReviewsForUser(1L, locationId, googleAuthManager);
    }

    @Test
    void getReviews_withUnauthorizedUser_shouldReturnForbidden() throws IOException {
        // Given
        String jwt = "Bearer " + generateValidToken(1L);
        String locationId = "location-123";
        String email = "unauthorized@example.com";

        when(reviewManager.getReviewsForUser(1L, locationId, googleAuthManager))
                .thenThrow(new UnauthorizedUserAccess("Not authorized", email));

        // When
        ResponseEntity<List<Review>> result = controller.getReviews(jwt, locationId, response);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        verify(reviewManager).getReviewsForUser(1L, locationId, googleAuthManager);
    }

    @Test
    void getLocations_withValidJwt_shouldReturnLocations() throws IOException, UnauthorizedUserAccess {
        // Given
        String jwt = "Bearer " + generateValidToken(1L);
        Map<String, Object> location = Map.of("name", "location-123", "title", "Test Location");

        when(reviewManager.getLocationsForUser(1L, googleAuthManager))
                .thenReturn(List.of(location));

        // When
        ResponseEntity<List<Map<String, Object>>> response = controller.getLocations(jwt, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(reviewManager).getLocationsForUser(1L, googleAuthManager);
    }

    @Test
    void getLocations_withUnauthorizedUser_shouldReturnForbidden() throws IOException {
        // Given
        String jwt = "Bearer " + generateValidToken(1L);
        String email = "unauthorized@example.com";

        when(reviewManager.getLocationsForUser(1L, googleAuthManager))
                .thenThrow(new UnauthorizedUserAccess("Not authorized", email));

        // When
        ResponseEntity<List<Map<String, Object>>> result = controller.getLocations(jwt, response);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        verify(reviewManager).getLocationsForUser(1L, googleAuthManager);
    }

    @Test
    void replyToReview_withValidData_shouldSucceed() throws IOException, UnauthorizedUserAccess {
        // Given
        String jwt = "Bearer " + generateValidToken(1L);
        String locationId = "location-123";
        String reviewId = "review-123";
        String replyText = "Thank you for your review!";
        noteZenReviewController.ReplyRequest request = new noteZenReviewController.ReplyRequest(replyText);

        // When
        ResponseEntity<Void> response = controller.replyToReview(jwt, locationId, reviewId, request, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reviewManager).replyToReview(1L, locationId, reviewId, replyText, googleAuthManager);
    }

    @Test
    void replyToReview_withUnauthorizedUser_shouldReturnForbidden() throws IOException {
        // Given
        String jwt = "Bearer " + generateValidToken(1L);
        String locationId = "location-123";
        String reviewId = "review-123";
        String email = "unauthorized@example.com";
        noteZenReviewController.ReplyRequest request = new noteZenReviewController.ReplyRequest("text");

        doThrow(new UnauthorizedUserAccess("Not authorized", email))
                .when(reviewManager).replyToReview(1L, locationId, reviewId, "text", googleAuthManager);

        // When
        ResponseEntity<Void> result = controller.replyToReview(jwt, locationId, reviewId, request, response);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    private String generateValidToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}
