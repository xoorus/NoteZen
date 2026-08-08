package com.bchev.notezen.config;

import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Maintient à jour les 2 plans d'abonnement (Starter/Professional) à chaque démarrage,
 * à partir des price IDs Stripe configurés en YAML. Upsert par nom : idempotent, une
 * seule ligne par plan, fonctionne aussi bien sur le H2 local (recréé à chaque
 * redémarrage) que sur la base Postgres de prod (jamais recréée).
 */
@Configuration
@RequiredArgsConstructor
public class SubscriptionPlanSeeder {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Value("${stripe.pricing.starter.stripePriceId}")
    private String starterPriceId;

    @Value("${stripe.pricing.starter.trialDays}")
    private Integer starterTrialDays;

    @Value("${stripe.pricing.professional.stripePriceId}")
    private String professionalPriceId;

    @Value("${stripe.pricing.professional.trialDays}")
    private Integer professionalTrialDays;

    @Bean
    CommandLineRunner seedSubscriptionPlans() {
        return args -> {
            upsert("Starter", new BigDecimal("24.90"), 1, starterPriceId, starterTrialDays);
            upsert("Professional", new BigDecimal("34.90"), 3, professionalPriceId, professionalTrialDays);
        };
    }

    private void upsert(String name, BigDecimal price, int maxLocations, String stripePriceId, Integer trialDays) {
        SubscriptionPlanEntity plan = subscriptionPlanRepository.findByName(name)
                .orElseGet(SubscriptionPlanEntity::new);

        plan.setName(name);
        plan.setPrice(price);
        plan.setCurrency("EUR");
        plan.setInterval("month");
        plan.setMaxLocations(maxLocations);
        plan.setStripePriceId(stripePriceId);
        plan.setTrialDays(trialDays);
        plan.setActive(true);

        subscriptionPlanRepository.save(plan);
    }
}
