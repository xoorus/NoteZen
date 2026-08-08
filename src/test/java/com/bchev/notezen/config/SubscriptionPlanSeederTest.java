package com.bchev.notezen.config;

import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanSeederTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    private SubscriptionPlanSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new SubscriptionPlanSeeder(subscriptionPlanRepository);
        ReflectionTestUtils.setField(seeder, "starterPriceId", "price_starter_new");
        ReflectionTestUtils.setField(seeder, "starterTrialDays", 7);
        ReflectionTestUtils.setField(seeder, "professionalPriceId", "price_pro_new");
        ReflectionTestUtils.setField(seeder, "professionalTrialDays", 7);
    }

    @Test
    void seed_firstRun_shouldCreateBothPlansAsActive() throws Exception {
        when(subscriptionPlanRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommandLineRunner runner = seeder.seedSubscriptionPlans();
        runner.run();

        verify(subscriptionPlanRepository, times(2))
                .save(argThat(p -> ((SubscriptionPlanEntity) p).getActive()));
    }

    @Test
    void seed_priceChangedForExistingPlan_shouldUpdateSameRowInPlace() throws Exception {
        SubscriptionPlanEntity existingStarter = SubscriptionPlanEntity.builder()
                .id(1L).name("Starter").stripePriceId("price_starter_old")
                .price(new BigDecimal("19.90")).active(true).build();

        when(subscriptionPlanRepository.findByName("Starter")).thenReturn(Optional.of(existingStarter));
        when(subscriptionPlanRepository.findByName("Professional")).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommandLineRunner runner = seeder.seedSubscriptionPlans();
        runner.run();

        assertEquals(1L, existingStarter.getId());
        assertEquals("price_starter_new", existingStarter.getStripePriceId());
        assertEquals(0, new BigDecimal("24.90").compareTo(existingStarter.getPrice()));
        verify(subscriptionPlanRepository, never()).delete(any());
    }
}
