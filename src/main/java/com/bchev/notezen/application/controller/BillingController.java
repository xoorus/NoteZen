package com.bchev.notezen.application.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.bchev.notezen.domain.entity.SubscriptionEntity;
import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.StripeService;
import com.bchev.notezen.domain.service.SubscriptionManager;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import com.bchev.notezen.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final StripeService stripeService;
    private final SubscriptionManager subscriptionManager;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @Value("${stripe.pricing.monthly.stripePriceId}")
    private String stripePriceId;

    @Value("${stripe.pricing.monthly.trialDays:14}")
    private Integer trialDays;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestHeader("Authorization") String jwt) {
        try {
            Long userId = TokenUtils.getUserIdFrom(jwt);
            Optional<UserEntity> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Vérifier si subscription existe déjà
            Optional<SubscriptionEntity> existingSubscription =
                    subscriptionRepository.findByUser(user.get());

            if (existingSubscription.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User already has an active subscription");
                return ResponseEntity.ok(response);
            }

            // Récupérer le plan actif
            Optional<SubscriptionPlanEntity> plan = subscriptionPlanRepository.findByActiveTrue();
            if (plan.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Subscription plan not found"));
            }

            // Créer subscription
            SubscriptionEntity subscription = subscriptionManager.startSubscription(
                    user.get(),
                    plan.get(),
                    trialDays
            );

            Map<String, Object> response = new HashMap<>();
            response.put("subscriptionId", subscription.getId());
            response.put("stripeSubscriptionId", subscription.getStripeSubscriptionId());
            response.put("status", subscription.getStatus());
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Stripe error during checkout", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Stripe error: " + e.getMessage()));
        }
    }

    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription(@RequestHeader("Authorization") String jwt) {
        try {
            Long userId = TokenUtils.getUserIdFrom(jwt);
            Optional<UserEntity> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<SubscriptionEntity> subscription = subscriptionRepository.findByUser(user.get());

            if (subscription.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No subscription found"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("subscriptionId", subscription.get().getId());
            response.put("status", subscription.get().getStatus());
            response.put("currentPeriodStart", subscription.get().getCurrentPeriodStart());
            response.put("currentPeriodEnd", subscription.get().getCurrentPeriodEnd());
            response.put("trialEndDate", subscription.get().getTrialEndDate());
            response.put("planName", subscription.get().getSubscriptionPlan().getName());
            response.put("price", subscription.get().getSubscriptionPlan().getPrice());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching subscription", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(@RequestHeader("Authorization") String jwt) {
        try {
            Long userId = TokenUtils.getUserIdFrom(jwt);
            Optional<UserEntity> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<SubscriptionEntity> subscription = subscriptionRepository.findByUser(user.get());

            if (subscription.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No subscription found"));
            }

            stripeService.cancelSubscription(subscription.get().getStripeSubscriptionId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Subscription canceled");
            response.put("subscriptionId", subscription.get().getId());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Stripe error during cancellation", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Stripe error: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        try {
            Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);

            switch (event.getType()) {
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                default:
                    log.info("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("Webhook processing error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid webhook signature"));
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) throws StripeException {
        com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (invoice != null && invoice.getId() != null) {
            subscriptionManager.handlePaymentSuccess(invoice.getId());
        }
    }

    private void handleInvoicePaymentFailed(Event event) throws StripeException {
        com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (invoice != null && invoice.getId() != null) {
            String failureReason = invoice.getLastFinalizationError() != null ?
                    invoice.getLastFinalizationError().getMessage() : "Unknown failure";
            subscriptionManager.handlePaymentFailure(invoice.getId(), failureReason);
        }
    }

    private void handleSubscriptionCreated(Event event) {
        com.stripe.model.Subscription subscription = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

        if (subscription != null) {
            log.info("Subscription created: {}", subscription.getId());
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription subscription = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

        if (subscription != null) {
            Optional<SubscriptionEntity> dbSubscription =
                    subscriptionRepository.findByStripeSubscriptionId(subscription.getId());
            if (dbSubscription.isPresent()) {
                dbSubscription.get().setStatus(subscription.getStatus());
                subscriptionRepository.save(dbSubscription.get());
                log.info("Subscription updated: {} status: {}", subscription.getId(), subscription.getStatus());
            }
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription subscription = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

        if (subscription != null) {
            Optional<SubscriptionEntity> dbSubscription =
                    subscriptionRepository.findByStripeSubscriptionId(subscription.getId());
            if (dbSubscription.isPresent()) {
                dbSubscription.get().setStatus("canceled");
                subscriptionRepository.save(dbSubscription.get());
                log.info("Subscription deleted: {}", subscription.getId());
            }
        }
    }
}
