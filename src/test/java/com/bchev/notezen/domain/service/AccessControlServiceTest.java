package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.entity.SubscriptionEntity;
import com.bchev.notezen.domain.exception.PaymentFailedException;
import com.bchev.notezen.domain.exception.SubscriptionCanceledException;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionManager subscriptionManager;

    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService(userRepository, subscriptionManager);
    }

    private UserEntity userWithEmail(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        return user;
    }

    private SubscriptionEntity subscriptionWithStatus(String status) {
        return SubscriptionEntity.builder().status(status).build();
    }

    @Test
    void isAuthorized_withAllowlistedEmail_shouldReturnTrueWithoutHittingSubscription() {
        assertTrue(accessControlService.isAuthorized("admin@notezen.fr"));
        assertTrue(accessControlService.isAuthorized("dev@notezen.fr"));
        assertTrue(accessControlService.isAuthorized("bchevriaut@gmail.com"));

        verifyNoInteractions(userRepository, subscriptionManager);
    }

    @Test
    void isAuthorized_allowlistCaseInsensitive_shouldReturnTrue() {
        assertTrue(accessControlService.isAuthorized("ADMIN@NOTEZEN.FR"));
        assertTrue(accessControlService.isAuthorized("Admin@NoteZen.fr"));
        assertTrue(accessControlService.isAuthorized("BCHEVRIAUT@GMAIL.COM"));
    }

    @Test
    void isAuthorized_userNotFound_shouldReturnFalse() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertFalse(accessControlService.isAuthorized("unknown@example.com"));
    }

    @Test
    void isAuthorized_noSubscription_shouldReturnFalse() {
        UserEntity user = userWithEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(subscriptionManager.getUserActiveSubscription(user)).thenReturn(Optional.empty());

        assertFalse(accessControlService.isAuthorized("user@example.com"));
    }

    @Test
    void isAuthorized_activeSubscription_shouldReturnTrue() {
        UserEntity user = userWithEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(subscriptionManager.getUserActiveSubscription(user))
                .thenReturn(Optional.of(subscriptionWithStatus("active")));

        assertTrue(accessControlService.isAuthorized("user@example.com"));
    }

    @Test
    void isAuthorized_trialingSubscription_shouldReturnTrue() {
        UserEntity user = userWithEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(subscriptionManager.getUserActiveSubscription(user))
                .thenReturn(Optional.of(subscriptionWithStatus("trialing")));

        assertTrue(accessControlService.isAuthorized("user@example.com"));
    }

    @Test
    void isAuthorized_pastDueSubscription_shouldThrowPaymentFailedException() {
        UserEntity user = userWithEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(subscriptionManager.getUserActiveSubscription(user))
                .thenReturn(Optional.of(subscriptionWithStatus("past_due")));

        assertThrows(PaymentFailedException.class,
                () -> accessControlService.isAuthorized("user@example.com"));
    }

    @Test
    void isAuthorized_canceledSubscription_shouldThrowSubscriptionCanceledException() {
        UserEntity user = userWithEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(subscriptionManager.getUserActiveSubscription(user))
                .thenReturn(Optional.of(subscriptionWithStatus("canceled")));

        assertThrows(SubscriptionCanceledException.class,
                () -> accessControlService.isAuthorized("user@example.com"));
    }

    @Test
    void isAuthorized_unknownStatus_shouldReturnFalse() {
        UserEntity user = userWithEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(subscriptionManager.getUserActiveSubscription(user))
                .thenReturn(Optional.of(subscriptionWithStatus("incomplete")));

        assertFalse(accessControlService.isAuthorized("user@example.com"));
    }
}
