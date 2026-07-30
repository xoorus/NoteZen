package com.bchev.notezen.config;

import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@Profile("local")
public class MockDataInitializer {

    @Value("${stripe.pricing.monthly.stripePriceId}")
    private String stripePriceId;

    @Value("${stripe.pricing.monthly.trialDays:14}")
    private Integer trialDays;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, SubscriptionPlanRepository subscriptionPlanRepository) {
        return args -> {
            userRepository.deleteAllInBatch();

            UserEntity mockUser = new UserEntity();
            mockUser.setEmail("dev@notezen.fr");
            mockUser.setGoogleAccountId("accounts/mock-user-123");
            mockUser.setGoogleAccessToken("fake-access-token");
            mockUser.setTokenExpiration(LocalDateTime.now().plusHours(10));

            userRepository.saveAndFlush(mockUser);
            System.out.println(">> [MOCK] Utilisateur de test créé avec ID: accounts/mock-user-123");

            if (subscriptionPlanRepository.findByActiveTrue().isEmpty()) {
                SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                        .name("NoteZen Premium")
                        .price(new BigDecimal("20.00"))
                        .currency("EUR")
                        .interval("monthly")
                        .trialDays(trialDays)
                        .stripePriceId(stripePriceId)
                        .active(true)
                        .build();
                subscriptionPlanRepository.saveAndFlush(plan);
                System.out.println(">> [MOCK] Plan d'abonnement seedé avec stripePriceId: " + stripePriceId);
            }
        };
    }
}