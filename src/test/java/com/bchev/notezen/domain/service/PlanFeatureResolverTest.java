package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.model.PricingPlan;
import com.bchev.notezen.domain.repository.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanFeatureResolverTest {

    private PlanFeatureResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PlanFeatureResolver();
    }

    @Test
    void getMaxLocations_withStarterPlan_shouldReturn1() {
        // Given
        UserEntity user = createUser(PricingPlan.STARTER);

        // When
        int max = resolver.getMaxLocations(user);

        // Then
        assertEquals(1, max);
    }

    @Test
    void getMaxLocations_withProfessionalPlan_shouldReturn999() {
        // Given
        UserEntity user = createUser(PricingPlan.PROFESSIONAL);

        // When
        int max = resolver.getMaxLocations(user);

        // Then
        assertEquals(999, max);
    }

    @Test
    void getMaxLocations_withNullUser_shouldReturnStarterDefault() {
        // When
        int max = resolver.getMaxLocations(null);

        // Then
        assertEquals(PricingPlan.STARTER.getMaxLocations(), max);
    }

    @Test
    void filterLocationsByPlan_starterUser_shouldLimit1Location() {
        // Given
        UserEntity user = createUser(PricingPlan.STARTER);
        List<Map<String, Object>> locations = List.of(
                Map.of("name", "loc-1", "title", "Location 1"),
                Map.of("name", "loc-2", "title", "Location 2"),
                Map.of("name", "loc-3", "title", "Location 3")
        );

        // When
        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(user, locations);

        // Then
        assertEquals(1, filtered.size());
        assertEquals("loc-1", filtered.get(0).get("name"));
    }

    @Test
    void filterLocationsByPlan_professionalUser_shouldReturnAllLocations() {
        // Given
        UserEntity user = createUser(PricingPlan.PROFESSIONAL);
        List<Map<String, Object>> locations = List.of(
                Map.of("name", "loc-1", "title", "Location 1"),
                Map.of("name", "loc-2", "title", "Location 2"),
                Map.of("name", "loc-3", "title", "Location 3")
        );

        // When
        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(user, locations);

        // Then
        assertEquals(3, filtered.size());
    }

    @Test
    void filterLocationsByPlan_withEmptyList_shouldReturnEmpty() {
        // Given
        UserEntity user = createUser(PricingPlan.STARTER);
        List<Map<String, Object>> locations = List.of();

        // When
        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(user, locations);

        // Then
        assertTrue(filtered.isEmpty());
    }

    @Test
    void filterLocationsByPlan_withNullList_shouldReturnNull() {
        // Given
        UserEntity user = createUser(PricingPlan.STARTER);

        // When
        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(user, null);

        // Then
        assertNull(filtered);
    }

    @Test
    void canAccessLocation_starterUser_shouldAllowOnlyFirstLocation() {
        // Given
        UserEntity user = createUser(PricingPlan.STARTER);

        // Then
        assertTrue(resolver.canAccessLocation(user, 0)); // First location
        assertFalse(resolver.canAccessLocation(user, 1)); // Second location
        assertFalse(resolver.canAccessLocation(user, 2)); // Third location
    }

    @Test
    void canAccessLocation_professionalUser_shouldAllowAllLocations() {
        // Given
        UserEntity user = createUser(PricingPlan.PROFESSIONAL);

        // Then
        assertTrue(resolver.canAccessLocation(user, 0));
        assertTrue(resolver.canAccessLocation(user, 1));
        assertTrue(resolver.canAccessLocation(user, 500)); // Professional has 999 max
    }

    @Test
    void getPlanName_shouldReturnCorrectName() {
        // Given
        UserEntity starterUser = createUser(PricingPlan.STARTER);
        UserEntity professionalUser = createUser(PricingPlan.PROFESSIONAL);

        // Then
        assertEquals("STARTER", resolver.getPlanName(starterUser));
        assertEquals("PROFESSIONAL", resolver.getPlanName(professionalUser));
    }

    @Test
    void getMonthlyPrice_shouldReturnCorrectPrice() {
        // Given
        UserEntity starterUser = createUser(PricingPlan.STARTER);
        UserEntity professionalUser = createUser(PricingPlan.PROFESSIONAL);

        // Then
        assertEquals(19.90, resolver.getMonthlyPrice(starterUser));
        assertEquals(24.90, resolver.getMonthlyPrice(professionalUser));
    }

    private UserEntity createUser(PricingPlan plan) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPricingPlan(plan);
        return user;
    }
}
