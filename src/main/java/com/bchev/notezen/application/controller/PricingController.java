package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.model.PricingPlan;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "https://www.notezen.fr", "https://notezen.fr"})
@RequiredArgsConstructor
public class PricingController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    @Value("${front.url}")
    private String frontUrl;

    @GetMapping("/plans")
    public ResponseEntity<List<Map<String, Object>>> getPlans() {
        log.info("[PricingController] Fetching available pricing plans");

        List<Map<String, Object>> plans = List.of(
                Map.of(
                        "id", PricingPlan.STARTER.name(),
                        "name", "Starter",
                        "price", PricingPlan.STARTER.getMonthlyPrice(),
                        "currency", "EUR",
                        "interval", "month",
                        "maxLocations", PricingPlan.STARTER.getMaxLocations(),
                        "stripePriceId", PricingPlan.STARTER.getStripePriceId(),
                        "features", List.of(
                                "1 établissement",
                                "Avis illimités",
                                "Réponses illimitées"
                        )
                ),
                Map.of(
                        "id", PricingPlan.PROFESSIONAL.name(),
                        "name", "Professional",
                        "price", PricingPlan.PROFESSIONAL.getMonthlyPrice(),
                        "currency", "EUR",
                        "interval", "month",
                        "maxLocations", PricingPlan.PROFESSIONAL.getMaxLocations(),
                        "stripePriceId", PricingPlan.PROFESSIONAL.getStripePriceId(),
                        "features", List.of(
                                "Établissements illimités",
                                "Avis illimités",
                                "Réponses illimitées",
                                "Analyse avancée"
                        )
                )
        );

        return ResponseEntity.ok(plans);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(
            @RequestHeader("Authorization") String jwt,
            @RequestBody CheckoutRequest request) {
        try {
            log.info("[PricingController] Creating checkout session for plan: {}", request.getPlanId());

            // Parse JWT to get user ID (simplified - in prod use proper JWT parsing)
            // For now, we assume the JWT is validated by another layer
            Long userId = request.getUserId(); // Frontend sends user ID

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Find the plan
            PricingPlan plan = PricingPlan.valueOf(request.getPlanId());

            // Create or get Stripe customer
            String stripeCustomerId = user.getStripeCustomerId();
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                var customer = stripeService.createCustomer(user.getEmail(), user.getEmail());
                stripeCustomerId = customer.getId();
                user.setStripeCustomerId(stripeCustomerId);
                userRepository.save(user);
            }

            // Create checkout session
            String successUrl = frontUrl + "dashboard?session_id={CHECKOUT_SESSION_ID}&plan=" + plan.name();
            String cancelUrl = frontUrl + "pricing";

            String checkoutUrl = stripeService.createCheckoutSession(
                    stripeCustomerId,
                    plan.getStripePriceId(),
                    7, // trial days
                    userId.toString(),
                    successUrl,
                    cancelUrl
            );

            log.info("[PricingController] Checkout session created: {}", checkoutUrl);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));

        } catch (StripeException e) {
            log.error("[PricingController] Stripe error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create checkout session"));
        } catch (IllegalArgumentException e) {
            log.error("[PricingController] Invalid plan: {}", request.getPlanId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid plan selected"));
        }
    }

    public static class CheckoutRequest {
        private String planId;
        private Long userId;

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
