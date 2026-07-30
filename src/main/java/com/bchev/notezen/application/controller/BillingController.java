package com.bchev.notezen.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @Value("${front.url}")
    private String frontUrl;

    /**
     * Crée une session Stripe Checkout hébergée. L'utilisateur y saisit sa carte ;
     * aucune SubscriptionEntity n'est créée ici, elle ne l'est que lorsque le webhook
     * checkout.session.completed confirme que le paiement a été configuré côté Stripe.
     * Point d'entrée pour un utilisateur déjà connecté qui relance un paiement
     * (ex: depuis /unauthorized après un 401) ; le flow principal passe par
     * GoogleBusinessController qui appelle directement SubscriptionManager.startCheckout.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestHeader("Authorization") String jwt) {
        try {
            Long userId = TokenUtils.getUserIdFrom(jwt);
            Optional<UserEntity> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (subscriptionManager.hasActiveSubscription(user.get())) {
                return ResponseEntity.ok(Map.of("message", "User already has an active subscription"));
            }

            String checkoutUrl = subscriptionManager.startCheckout(
                    user.get(),
                    frontUrl + "dashboard?token=" + jwt.replace("Bearer ", "").trim(),
                    frontUrl + "dashboard?token=" + jwt.replace("Bearer ", "").trim()
            );

            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));

        } catch (IllegalStateException e) {
            log.error("Billing configuration error", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
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

    /**
     * Les champs métier sont lus directement dans le JSON brut du webhook plutôt
     * que via la désérialisation typée de stripe-java (event.getDataObjectDeserializer()) :
     * cette dernière retourne un objet vide/partiel dès que la version d'API Stripe
     * du compte diverge de celle pinée dans le SDK — piège connu, indépendant de notre code.
     * La lecture JSON brute contourne totalement le problème.
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        try {
            Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
            JsonNode dataObject = objectMapper.readTree(payload).path("data").path("object");

            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(dataObject);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(dataObject);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(dataObject);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(dataObject);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(dataObject);
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

    /**
     * Point d'entrée unique de création d'une SubscriptionEntity : le paiement
     * est confirmé côté Stripe (carte attachée), donc la subscription est réelle.
     */
    private void handleCheckoutSessionCompleted(JsonNode session) throws StripeException {
        String clientReferenceId = textOrNull(session, "client_reference_id");
        String stripeSubscriptionId = textOrNull(session, "subscription");
        String stripeCustomerId = textOrNull(session, "customer");

        if (clientReferenceId == null || stripeSubscriptionId == null) {
            log.warn("Checkout session completed without client_reference_id or subscription");
            return;
        }

        if (subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).isPresent()) {
            log.info("Subscription {} already persisted", stripeSubscriptionId);
            return;
        }

        Long userId = Long.parseLong(clientReferenceId);
        Optional<UserEntity> user = userRepository.findById(userId);
        Optional<SubscriptionPlanEntity> plan = subscriptionPlanRepository.findByActiveTrue();

        if (user.isEmpty() || plan.isEmpty()) {
            log.error("Cannot persist subscription {}: user {} or plan missing", stripeSubscriptionId, userId);
            return;
        }

        com.stripe.model.Subscription stripeSubscription = stripeService.getSubscription(stripeSubscriptionId);
        subscriptionManager.persistSubscriptionFromCheckout(
                user.get(), plan.get(), stripeSubscription, stripeCustomerId);
    }

    private void handleInvoicePaymentSucceeded(JsonNode invoice) throws StripeException {
        String invoiceId = textOrNull(invoice, "id");
        if (invoiceId != null) {
            subscriptionManager.handlePaymentSuccess(invoiceId);
        }
    }

    private void handleInvoicePaymentFailed(JsonNode invoice) throws StripeException {
        String invoiceId = textOrNull(invoice, "id");
        if (invoiceId != null) {
            subscriptionManager.handlePaymentFailure(invoiceId, "Payment failed");
        }
    }

    private void handleSubscriptionUpdated(JsonNode subscription) {
        String stripeSubscriptionId = textOrNull(subscription, "id");
        String status = textOrNull(subscription, "status");
        if (stripeSubscriptionId == null || status == null) {
            return;
        }

        Optional<SubscriptionEntity> dbSubscription =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        if (dbSubscription.isPresent()) {
            dbSubscription.get().setStatus(status);
            subscriptionRepository.save(dbSubscription.get());
            log.info("Subscription updated: {} status: {}", stripeSubscriptionId, status);
        }
    }

    private void handleSubscriptionDeleted(JsonNode subscription) {
        String stripeSubscriptionId = textOrNull(subscription, "id");
        if (stripeSubscriptionId == null) {
            return;
        }

        Optional<SubscriptionEntity> dbSubscription =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        if (dbSubscription.isPresent()) {
            dbSubscription.get().setStatus("canceled");
            subscriptionRepository.save(dbSubscription.get());
            log.info("Subscription deleted: {}", stripeSubscriptionId);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
