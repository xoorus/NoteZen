package com.bchev.notezen.domain.service;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.exception.UnauthorizedUserAccess;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewManagerSimpleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleReviewManager googleReviewManager;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private GoogleAuthManager googleAuthManager;

    private ReviewManager reviewManager;

    @BeforeEach
    void setUp() {
        reviewManager = new ReviewManager(userRepository, googleReviewManager, accessControlService);
    }

    @Test
    void getLocationsForUser_withAuthorizedUser_shouldCallGoogleReviewManager() throws UnauthorizedUserAccess {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setGoogleAccountId("account-123");

        Map<String, Object> location = Map.of("name", "loc-1", "title", "Test Location");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessControlService.isAuthorized(email)).thenReturn(true);
        when(googleReviewManager.getLocations(user, googleAuthManager))
                .thenReturn(List.of(location));

        // When
        List<Map<String, Object>> locations = reviewManager.getLocationsForUser(userId, googleAuthManager);

        // Then
        assertNotNull(locations);
        assertEquals(1, locations.size());
        verify(googleReviewManager).getLocations(user, googleAuthManager);
        verify(accessControlService).isAuthorized(email);
    }

    @Test
    void replyToReview_withValidUser_shouldCallGoogleReviewManager() throws UnauthorizedUserAccess {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setGoogleAccountId("account-123");

        String locationId = "location-123";
        String reviewId = "review-123";
        String replyText = "Thank you!";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessControlService.isAuthorized(email)).thenReturn(true);

        // When
        reviewManager.replyToReview(userId, locationId, reviewId, replyText, googleAuthManager);

        // Then
        verify(googleReviewManager).replyToReview(user, locationId, reviewId, replyText, googleAuthManager);
        assertEquals(1, user.getRepliesPostedCount());
        verify(userRepository).save(user);
    }

    @Test
    void replyToReview_whenGoogleReviewManagerThrows_shouldNotIncrementCounter() throws UnauthorizedUserAccess {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setGoogleAccountId("account-123");

        String locationId = "location-123";
        String reviewId = "review-123";
        String replyText = "Thank you!";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessControlService.isAuthorized(email)).thenReturn(true);
        doThrow(new RuntimeException("Échec de la publication de la réponse Google"))
                .when(googleReviewManager).replyToReview(user, locationId, reviewId, replyText, googleAuthManager);

        // When & Then
        assertThrows(RuntimeException.class,
                () -> reviewManager.replyToReview(userId, locationId, reviewId, replyText, googleAuthManager));

        assertEquals(0, user.getRepliesPostedCount());
        verify(userRepository, never()).save(user);
    }

    @Test
    void getLocationsForUser_withUnauthorizedUser_shouldThrowException() {
        // Given
        Long userId = 1L;
        String email = "unauthorized@example.com";
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setGoogleAccountId("account-123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessControlService.isAuthorized(email)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedUserAccess.class,
                () -> reviewManager.getLocationsForUser(userId, googleAuthManager));

        verify(googleReviewManager, never()).getLocations(any(), any());
    }

    @Test
    void getLocationsForUser_withNoGoogleAccount_shouldReturnEmptyList() throws UnauthorizedUserAccess {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setGoogleAccountId(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessControlService.isAuthorized(email)).thenReturn(true);

        // When
        List<Map<String, Object>> locations = reviewManager.getLocationsForUser(userId, googleAuthManager);

        // Then
        assertTrue(locations.isEmpty());
        verify(googleReviewManager, never()).getLocations(any(), any());
    }
}
