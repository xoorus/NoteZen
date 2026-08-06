package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.model.PricingPlan;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/webhook/stripe")
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookController {

    private final UserRepository userRepository;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<?> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("[StripeWebhook] Received event type: {}", event.getType());

            switch (event.getType()) {
                case "customer.subscription.updated" -> handleSubscriptionUpdated((Subscription) event.getDataObjectDeserializer().getObject().orElse(null));
                case "customer.subscription.deleted" -> handleSubscriptionDeleted((Subscription) event.getDataObjectDeserializer().getObject().orElse(null));
                case "customer.subscription.created" -> handleSubscriptionCreated((Subscription) event.getDataObjectDeserializer().getObject().orElse(null));
                default -> log.debug("[StripeWebhook] Unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok("Received");

        } catch (Exception e) {
            log.error("[StripeWebhook] Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }
    }

    private void handleSubscriptionCreated(Subscription subscription) {
        if (subscription == null) {
            log.warn("[StripeWebhook] Subscription is null for created event");
            return;
        }

        log.info("[StripeWebhook] Subscription created: {}", subscription.getId());

        // Find user by stripe customer ID
        UserEntity user = userRepository.findAll().stream()
                .filter(u -> subscription.getCustomer().equals(u.getStripeCustomerId()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("[StripeWebhook] User not found for customer: {}", subscription.getCustomer());
            return;
        }

        // Update subscription details
        user.setStripeSubscriptionId(subscription.getId());

        // Extract end date from subscription
        if (subscription.getCurrentPeriodEnd() != null) {
            LocalDateTime endDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()),
                    ZoneId.systemDefault()
            );
            user.setSubscriptionEndDate(endDate);
        }

        // Update pricing plan based on price
        String priceId = subscription.getItems().getData().get(0).getPrice().getId();
        PricingPlan plan = getPlanFromPriceId(priceId);
        user.setPricingPlan(plan);

        userRepository.save(user);
        log.info("[StripeWebhook] Updated user {} with subscription {} and plan {}", user.getId(), subscription.getId(), plan);
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        if (subscription == null) {
            log.warn("[StripeWebhook] Subscription is null for updated event");
            return;
        }

        log.info("[StripeWebhook] Subscription updated: {}", subscription.getId());

        // Find user by subscription ID
        UserEntity user = userRepository.findAll().stream()
                .filter(u -> subscription.getId().equals(u.getStripeSubscriptionId()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("[StripeWebhook] User not found for subscription: {}", subscription.getId());
            return;
        }

        // Update pricing plan if changed
        if (subscription.getItems() != null && !subscription.getItems().getData().isEmpty()) {
            String priceId = subscription.getItems().getData().get(0).getPrice().getId();
            PricingPlan plan = getPlanFromPriceId(priceId);
            user.setPricingPlan(plan);
        }

        // Update end date
        if (subscription.getCurrentPeriodEnd() != null) {
            LocalDateTime endDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()),
                    ZoneId.systemDefault()
            );
            user.setSubscriptionEndDate(endDate);
        }

        userRepository.save(user);
        log.info("[StripeWebhook] Updated user {} with plan {}", user.getId(), user.getPricingPlan());
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        if (subscription == null) {
            log.warn("[StripeWebhook] Subscription is null for deleted event");
            return;
        }

        log.info("[StripeWebhook] Subscription deleted: {}", subscription.getId());

        UserEntity user = userRepository.findAll().stream()
                .filter(u -> subscription.getId().equals(u.getStripeSubscriptionId()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("[StripeWebhook] User not found for subscription: {}", subscription.getId());
            return;
        }

        // Reset to STARTER plan
        user.setPricingPlan(PricingPlan.STARTER);
        user.setStripeSubscriptionId(null);
        user.setSubscriptionEndDate(null);

        userRepository.save(user);
        log.info("[StripeWebhook] Downgraded user {} to STARTER after subscription cancellation", user.getId());
    }

    private PricingPlan getPlanFromPriceId(String priceId) {
        return switch (priceId) {
            case "price_1TywUBJCNseoIpqDAFOOvnhb" -> PricingPlan.STARTER; // test
            case "price_1U1NyAJCNseoIpqDOR3rLQC5" -> PricingPlan.PROFESSIONAL; // test
            case "price_1U1O6cR08oYxISpqIqD1IWL2" -> PricingPlan.PROFESSIONAL; // prod
            default -> {
                log.warn("[StripeWebhook] Unknown price ID: {}", priceId);
                yield PricingPlan.STARTER;
            }
        };
    }
}
