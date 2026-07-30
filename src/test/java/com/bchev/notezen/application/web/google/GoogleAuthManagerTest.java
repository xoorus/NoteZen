package com.bchev.notezen.application.web.google;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.BusinessProvider;
import com.bchev.notezen.domain.service.BusinessProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthManagerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private BusinessProviderResolver businessProviderResolver;

    @Mock
    private BusinessProvider businessProvider;

    private GoogleAuthManager googleAuthManager;

    @BeforeEach
    void setUp() {
        googleAuthManager = new GoogleAuthManager(
                userRepository,
                googleAuthService,
                businessProviderResolver
        );
    }

    @Test
    void linkAccount_newUser_shouldCreateAndSaveUserRegardlessOfAuthorization() {
        // linkAccount ne vérifie plus l'autorisation métier : cette responsabilité
        // a été déplacée vers GoogleBusinessController, qui décide seulement de la
        // redirection (dashboard vs unauthorized) une fois le compte/JWT créés.
        String code = "auth-code-123";
        String email = "user@example.com";
        String idToken = createIdToken(email);
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-123";
        int expiresIn = 3600;

        GoogleTokenResponseDTO tokens = new GoogleTokenResponseDTO();
        tokens.setIdToken(idToken);
        tokens.setAccessToken(accessToken);
        tokens.setRefreshToken(refreshToken);
        tokens.setExpiresIn(expiresIn);

        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);

        when(googleAuthService.exchangeCodeForTokens(code)).thenReturn(tokens);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(businessProviderResolver.resolve(any())).thenReturn(businessProvider);
        when(businessProvider.fetchAccountId(accessToken)).thenReturn("account-123");
        when(userRepository.save(any())).thenReturn(newUser);

        // When
        UserEntity result = googleAuthManager.linkAccount(code);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(googleAuthService).exchangeCodeForTokens(code);
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void getValidToken_withValidToken_shouldReturnExistingToken() {
        // Given
        UserEntity user = createUser("user@example.com");
        user.setGoogleAccessToken("valid-token");
        user.setTokenExpiration(LocalDateTime.now().plusHours(2));

        // When
        String token = googleAuthManager.getValidToken(user);

        // Then
        assertEquals("valid-token", token);
        verify(googleAuthService, never()).refreshAccessToken(any());
    }

    @Test
    void getValidToken_withExpiredToken_shouldRefreshAndReturn() {
        // Given
        UserEntity user = createUser("user@example.com");
        user.setGoogleAccessToken("expired-token");
        user.setGoogleRefreshToken("refresh-token-123");
        user.setTokenExpiration(LocalDateTime.now().minusHours(1)); // Expired

        String newToken = "new-access-token";
        when(googleAuthService.refreshAccessToken("refresh-token-123")).thenReturn(newToken);
        when(userRepository.save(any())).thenReturn(user);

        // When
        String token = googleAuthManager.getValidToken(user);

        // Then
        assertEquals(newToken, token);
        verify(googleAuthService).refreshAccessToken("refresh-token-123");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void getValidToken_withExpiringTokenWithinMargin_shouldRefreshToken() {
        // Given
        UserEntity user = createUser("user@example.com");
        user.setGoogleAccessToken("soon-to-expire");
        user.setGoogleRefreshToken("refresh-token-123");
        user.setTokenExpiration(LocalDateTime.now().plusMinutes(3)); // Expires in 3 minutes

        String newToken = "new-access-token";
        when(googleAuthService.refreshAccessToken("refresh-token-123")).thenReturn(newToken);
        when(userRepository.save(any())).thenReturn(user);

        // When
        String token = googleAuthManager.getValidToken(user);

        // Then
        assertEquals(newToken, token);
        verify(googleAuthService).refreshAccessToken("refresh-token-123");
    }

    @Test
    void getValidToken_withNoRefreshToken_shouldThrowException() {
        // Given
        UserEntity user = createUser("user@example.com");
        user.setGoogleAccessToken("expired-token");
        user.setGoogleRefreshToken(null);
        user.setTokenExpiration(LocalDateTime.now().minusHours(1));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> googleAuthManager.getValidToken(user));

        verify(userRepository, never()).save(any());
    }

    @Test
    void linkAccount_withExistingUser_shouldUpdateTokens() {
        // Given
        String code = "auth-code-123";
        String email = "existing@example.com";
        String idToken = createIdToken(email);
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        GoogleTokenResponseDTO tokens = new GoogleTokenResponseDTO();
        tokens.setIdToken(idToken);
        tokens.setAccessToken(newAccessToken);
        tokens.setRefreshToken(newRefreshToken);
        tokens.setExpiresIn(3600);

        UserEntity existingUser = createUser(email);
        existingUser.setGoogleAccountId("account-123"); // Already linked

        when(googleAuthService.exchangeCodeForTokens(code)).thenReturn(tokens);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenReturn(existingUser);

        // When
        UserEntity result = googleAuthManager.linkAccount(code);

        // Then
        assertEquals(newAccessToken, result.getGoogleAccessToken());
        assertEquals(newRefreshToken, result.getGoogleRefreshToken());
        verify(businessProviderResolver, never()).resolve(any()); // Don't sync account if already present
    }

    private UserEntity createUser(String email) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail(email);
        return user;
    }

    private String createIdToken(String email) {
        return JWT.create()
                .withClaim("email", email)
                .sign(Algorithm.HMAC256("test-secret"));
    }
}
