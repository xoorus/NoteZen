package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.ReviewPage;
import com.bchev.notezen.domain.model.Reviewer;
import com.bchev.notezen.domain.model.StarRating;
import com.bchev.notezen.domain.repository.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleReviewManagerTest {

    @Mock
    private BusinessProviderResolver businessProviderResolver;

    @Mock
    private PlanFeatureResolver planFeatureResolver;

    @Mock
    private GoogleAuthManager googleAuthManager;

    @Mock
    private BusinessProvider mockProvider;

    @Mock
    private BusinessProvider realProvider;

    private GoogleReviewManager googleReviewManager;

    @BeforeEach
    void setUp() {
        googleReviewManager = new GoogleReviewManager(businessProviderResolver, planFeatureResolver);
    }

    @Test
    void getLocations_shouldResolveProviderAndFetchLocations() {
        // Given
        UserEntity user = createUser("user@example.com");

        when(googleAuthManager.getValidToken(user)).thenReturn("valid-token");
        when(businessProviderResolver.resolve(user)).thenReturn(mockProvider);
        List<Map<String, Object>> allLocations = List.of(
                Map.of("name", "loc-1", "title", "Location 1"),
                Map.of("name", "loc-2", "title", "Location 2")
        );
        when(mockProvider.fetchLocations("account-123", "valid-token"))
                .thenReturn(allLocations);
        when(planFeatureResolver.filterLocationsByPlan(user, allLocations))
                .thenReturn(allLocations); // Professional user sees all

        // When
        List<Map<String, Object>> locations = googleReviewManager.getLocations(user, googleAuthManager);

        // Then
        assertEquals(2, locations.size());
        verify(businessProviderResolver).resolve(user);
        verify(mockProvider).fetchLocations("account-123", "valid-token");
        verify(planFeatureResolver).filterLocationsByPlan(user, allLocations);
    }

    @Test
    void getLocations_withStarterUser_shouldFilterTo1Location() {
        // Given
        UserEntity user = createUser("starter@example.com");

        when(googleAuthManager.getValidToken(user)).thenReturn("token");
        when(businessProviderResolver.resolve(user)).thenReturn(realProvider);
        List<Map<String, Object>> allLocations = List.of(
                Map.of("name", "loc-1", "title", "Location 1"),
                Map.of("name", "loc-2", "title", "Location 2"),
                Map.of("name", "loc-3", "title", "Location 3")
        );
        when(realProvider.fetchLocations("account-123", "token")).thenReturn(allLocations);
        List<Map<String, Object>> filtered = List.of(allLocations.get(0)); // Only first
        when(planFeatureResolver.filterLocationsByPlan(user, allLocations))
                .thenReturn(filtered);

        // When
        List<Map<String, Object>> result = googleReviewManager.getLocations(user, googleAuthManager);

        // Then
        assertEquals(1, result.size());
        assertEquals("loc-1", result.get(0).get("name"));
        verify(planFeatureResolver).filterLocationsByPlan(user, allLocations);
    }

    @Test
    void getReviewsForUser_shouldFetchAllReviewsWithinTimeframe() {
        // Given
        UserEntity user = createUser("user@example.com");
        String locationId = "location-123";
        String token = "valid-token";

        when(googleAuthManager.getValidToken(user)).thenReturn(token);
        when(businessProviderResolver.resolve(user)).thenReturn(realProvider);

        // Create reviews with different timestamps
        Instant now = Instant.now();
        Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS); // Within 14 days
        Instant threeWeeksAgo = now.minus(21, ChronoUnit.DAYS); // Outside 14 days

        Review recentReview = createReview("1", "Recent review", now);
        Review withinTimeframeReview = createReview("2", "One week old (no reply)", oneWeekAgo);
        Review outsideTimeframeReview = createReview("3", "Three weeks old", threeWeeksAgo);

        ReviewPage page = new ReviewPage(List.of(recentReview, withinTimeframeReview, outsideTimeframeReview), null);
        when(realProvider.fetchReviews("account-123", locationId, token, null))
                .thenReturn(page);

        // When
        List<Review> reviews = googleReviewManager.getReviewsForUser(user, locationId, googleAuthManager);

        // Then
        assertEquals(2, reviews.size()); // Only reviews within 14 days timeframe
        verify(realProvider).fetchReviews("account-123", locationId, token, null);
    }

    @Test
    void getReviewsForUser_shouldFilterUnrepliedReviews() {
        // Given
        UserEntity user = createUser("user@example.com");
        String locationId = "location-123";
        String token = "valid-token";

        when(googleAuthManager.getValidToken(user)).thenReturn(token);
        when(businessProviderResolver.resolve(user)).thenReturn(realProvider);

        Instant now = Instant.now();
        Review unrepliedReview = createReview("1", "Unreplied", now);
        Review repliedReview = createReviewWithReply("2", "Replied", now);

        ReviewPage page = new ReviewPage(List.of(unrepliedReview, repliedReview), null);
        when(realProvider.fetchReviews("account-123", locationId, token, null))
                .thenReturn(page);

        // When
        List<Review> reviews = googleReviewManager.getReviewsForUser(user, locationId, googleAuthManager);

        // Then
        assertEquals(1, reviews.size());
        assertEquals("1", reviews.get(0).getReviewId());
        assertNull(reviews.get(0).getReviewReply());
    }

    @Test
    void getReviewsForUser_withPagination_shouldFetchMultiplePages() {
        // Given
        UserEntity user = createUser("user@example.com");
        String locationId = "location-123";
        String token = "valid-token";

        when(googleAuthManager.getValidToken(user)).thenReturn(token);
        when(businessProviderResolver.resolve(user)).thenReturn(realProvider);

        Instant now = Instant.now();
        Review review1 = createReview("1", "Review 1", now);
        Review review2 = createReview("2", "Review 2", now.minus(1, ChronoUnit.HOURS));

        ReviewPage page1 = new ReviewPage(List.of(review1), "next-page-token");
        ReviewPage page2 = new ReviewPage(List.of(review2), null);

        when(realProvider.fetchReviews("account-123", locationId, token, null))
                .thenReturn(page1);
        when(realProvider.fetchReviews("account-123", locationId, token, "next-page-token"))
                .thenReturn(page2);

        // When
        List<Review> reviews = googleReviewManager.getReviewsForUser(user, locationId, googleAuthManager);

        // Then
        assertEquals(2, reviews.size());
        verify(realProvider, times(2)).fetchReviews(
                eq("account-123"),
                eq(locationId),
                eq(token),
                any()
        );
    }

    @Test
    void replyToReview_shouldUseResolvedProvider() {
        // Given
        UserEntity user = createUser("user@example.com");
        String locationId = "location-123";
        String reviewId = "review-123";
        String replyText = "Thank you for your review!";
        String token = "valid-token";

        when(googleAuthManager.getValidToken(user)).thenReturn(token);
        when(businessProviderResolver.resolve(user)).thenReturn(realProvider);

        // When
        googleReviewManager.replyToReview(user, locationId, reviewId, replyText, googleAuthManager);

        // Then
        verify(businessProviderResolver).resolve(user);
        verify(realProvider).postReply("account-123", locationId, reviewId, replyText, token);
    }

    @Test
    void replyToReview_withMockUser_shouldUseMockProvider() {
        // Given
        UserEntity user = createUser("bchevriaut@gmail.com");
        String locationId = "location-123";
        String reviewId = "review-123";
        String replyText = "Mock reply";

        when(googleAuthManager.getValidToken(user)).thenReturn("token");
        when(businessProviderResolver.resolve(user)).thenReturn(mockProvider);

        // When
        googleReviewManager.replyToReview(user, locationId, reviewId, replyText, googleAuthManager);

        // Then
        verify(mockProvider).postReply("account-123", locationId, reviewId, replyText, "token");
    }

    @Test
    void getReviewsForUser_shouldStopPaginationWhenOutsideTimeframe() {
        // Given
        UserEntity user = createUser("user@example.com");
        String locationId = "location-123";
        String token = "valid-token";

        when(googleAuthManager.getValidToken(user)).thenReturn(token);
        when(businessProviderResolver.resolve(user)).thenReturn(realProvider);

        Instant now = Instant.now();
        Instant twoWeeksAgo = now.minus(14, ChronoUnit.DAYS);
        Instant threeWeeksAgo = now.minus(21, ChronoUnit.DAYS);

        Review recentReview = createReview("1", "Recent", now);
        Review oldReview = createReview("2", "Old (outside)", threeWeeksAgo);

        ReviewPage page = new ReviewPage(List.of(recentReview, oldReview), "next-page-token");
        when(realProvider.fetchReviews("account-123", locationId, token, null))
                .thenReturn(page);

        // When
        List<Review> reviews = googleReviewManager.getReviewsForUser(user, locationId, googleAuthManager);

        // Then
        assertEquals(1, reviews.size()); // Only recent review within 14 days
        // Should stop pagination when last review is outside timeframe
        verify(realProvider, times(1)).fetchReviews(any(), any(), any(), any());
    }

    private UserEntity createUser(String email) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail(email);
        user.setGoogleAccountId("account-123");
        return user;
    }

    private Review createReview(String id, String comment, Instant createTime) {
        return new Review(
                "Test Review", // name
                id, // reviewId
                new Reviewer("Test Author", "", false), // reviewer
                StarRating.FIVE, // starRating
                comment, // comment
                createTime.toString(), // createTime
                createTime.toString(), // updateTime
                null // reviewReply
        );
    }

    private Review createReviewWithReply(String id, String comment, Instant createTime) {
        return new Review(
                "Test Review", // name
                id, // reviewId
                new Reviewer("Test Author", "", false), // reviewer
                StarRating.FIVE, // starRating
                comment, // comment
                createTime.toString(), // createTime
                createTime.toString(), // updateTime
                new com.bchev.notezen.domain.model.ReviewReply("Reply comment", createTime.toString()) // reviewReply (non-null)
        );
    }
}
