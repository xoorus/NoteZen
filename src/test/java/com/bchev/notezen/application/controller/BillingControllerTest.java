package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.entity.SubscriptionEntity;
import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import com.bchev.notezen.domain.repository.SubscriptionRepository;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.StripeService;
import com.bchev.notezen.domain.service.SubscriptionManager;
import com.stripe.exception.CardException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    private static final String TEST_JWT_SECRET = "test-secret-key-for-jwt-testing-1234567890ab";
    private static final String WEBHOOK_SECRET = "whsec_test_secret";

    @Mock
    private StripeService stripeService;
    @Mock
    private SubscriptionManager subscriptionManager;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock
    private UserRepository userRepository;

    private BillingController controller;

    @BeforeEach
    void setUp() {
        controller = new BillingController(
                stripeService, subscriptionManager, subscriptionRepository, subscriptionPlanRepository, userRepository);

        ReflectionTestUtils.setField(TokenUtils.class, "secretKey", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(controller, "stripeWebhookSecret", WEBHOOK_SECRET);
        ReflectionTestUtils.setField(controller, "stripePriceId", "price_123");
        ReflectionTestUtils.setField(controller, "trialDays", 14);
    }

    private String tokenFor(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000))
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    private String signedPayload(String payload) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String signedContent = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedContent.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "t=" + timestamp + ",v1=" + hex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void checkout_userNotFound_shouldReturnUnauthorized() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.checkout(tokenFor(userId));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void checkout_existingSubscription_shouldReturnAlreadySubscribedMessage() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user))
                .thenReturn(Optional.of(SubscriptionEntity.builder().build()));

        ResponseEntity<?> response = controller.checkout(tokenFor(userId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User already has an active subscription", ((Map<?, ?>) response.getBody()).get("message"));
        verifyNoInteractions(subscriptionManager);
    }

    @Test
    void checkout_noPlanFound_shouldReturnNotFound() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.checkout(tokenFor(userId));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void checkout_success_shouldStartSubscriptionAndReturnDetails() throws Exception {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().stripePriceId("price_123").build();
        SubscriptionEntity created = SubscriptionEntity.builder()
                .id(10L)
                .stripeSubscriptionId("sub_123")
                .status("trialing")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.of(plan));
        when(subscriptionManager.startSubscription(user, plan, 14)).thenReturn(created);

        ResponseEntity<?> response = controller.checkout(tokenFor(userId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(10L, body.get("subscriptionId"));
        assertEquals("sub_123", body.get("stripeSubscriptionId"));
        assertEquals("trialing", body.get("status"));
    }

    @Test
    void checkout_stripeException_shouldReturnBadRequest() throws Exception {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().stripePriceId("price_123").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.of(plan));
        when(subscriptionManager.startSubscription(any(), any(), any()))
                .thenThrow(new CardException("Card declined", "req_1", null, null, null, null, null, null));

        ResponseEntity<?> response = controller.checkout(tokenFor(userId));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getSubscription_userNotFound_shouldReturnUnauthorized() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getSubscription(tokenFor(userId));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getSubscription_noSubscription_shouldReturnNotFound() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getSubscription(tokenFor(userId));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getSubscription_success_shouldReturnSubscriptionDetails() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().name("Pro").price(new java.math.BigDecimal("9.99")).build();
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(5L)
                .status("active")
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .subscriptionPlan(plan)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(subscription));

        ResponseEntity<?> response = controller.getSubscription(tokenFor(userId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("active", body.get("status"));
        assertEquals("Pro", body.get("planName"));
    }

    @Test
    void cancelSubscription_noSubscription_shouldReturnNotFound() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.cancelSubscription(tokenFor(userId));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void cancelSubscription_success_shouldCancelAndReturnMessage() throws Exception {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(5L)
                .stripeSubscriptionId("sub_123")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(subscription));

        ResponseEntity<?> response = controller.cancelSubscription(tokenFor(userId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(stripeService).cancelSubscription("sub_123");
    }

    @Test
    void cancelSubscription_stripeException_shouldReturnBadRequest() throws Exception {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(5L)
                .stripeSubscriptionId("sub_123")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(subscription));
        doThrow(new CardException("Card declined", "req_1", null, null, null, null, null, null))
                .when(stripeService).cancelSubscription("sub_123");

        ResponseEntity<?> response = controller.cancelSubscription(tokenFor(userId));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleWebhook_invalidSignature_shouldReturnBadRequest() {
        ResponseEntity<?> response = controller.handleWebhook("{}", "t=1,v1=invalid");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleWebhook_subscriptionUpdated_shouldSyncStatusInDb() {
        String payload = "{"
                + "\"id\":\"evt_1\","
                + "\"object\":\"event\","
                + "\"api_version\":\"" + com.stripe.Stripe.API_VERSION + "\","
                + "\"type\":\"customer.subscription.updated\","
                + "\"data\":{\"object\":{\"id\":\"sub_123\",\"object\":\"subscription\",\"status\":\"past_due\"}}"
                + "}";
        SubscriptionEntity dbSubscription = SubscriptionEntity.builder()
                .stripeSubscriptionId("sub_123")
                .status("active")
                .build();
        when(subscriptionRepository.findByStripeSubscriptionId("sub_123"))
                .thenReturn(Optional.of(dbSubscription));

        ResponseEntity<?> response = controller.handleWebhook(payload, signedPayload(payload));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("past_due", dbSubscription.getStatus());
        verify(subscriptionRepository).save(dbSubscription);
    }

    @Test
    void handleWebhook_subscriptionDeleted_shouldMarkCanceledInDb() {
        String payload = "{"
                + "\"id\":\"evt_2\","
                + "\"object\":\"event\","
                + "\"api_version\":\"" + com.stripe.Stripe.API_VERSION + "\","
                + "\"type\":\"customer.subscription.deleted\","
                + "\"data\":{\"object\":{\"id\":\"sub_123\",\"object\":\"subscription\",\"status\":\"canceled\"}}"
                + "}";
        SubscriptionEntity dbSubscription = SubscriptionEntity.builder()
                .stripeSubscriptionId("sub_123")
                .status("active")
                .build();
        when(subscriptionRepository.findByStripeSubscriptionId("sub_123"))
                .thenReturn(Optional.of(dbSubscription));

        ResponseEntity<?> response = controller.handleWebhook(payload, signedPayload(payload));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("canceled", dbSubscription.getStatus());
    }

    @Test
    void handleWebhook_unhandledEventType_shouldStillReturnOk() {
        String payload = "{"
                + "\"id\":\"evt_3\","
                + "\"object\":\"event\","
                + "\"api_version\":\"" + com.stripe.Stripe.API_VERSION + "\","
                + "\"type\":\"payment_intent.succeeded\","
                + "\"data\":{\"object\":{\"id\":\"pi_123\",\"object\":\"payment_intent\"}}"
                + "}";

        ResponseEntity<?> response = controller.handleWebhook(payload, signedPayload(payload));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(subscriptionManager);
    }
}
