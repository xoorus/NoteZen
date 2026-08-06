package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.model.PricingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "https://www.notezen.fr", "https://notezen.fr"})
public class PricingController {

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
}
