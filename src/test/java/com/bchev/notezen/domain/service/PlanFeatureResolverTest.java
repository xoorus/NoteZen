package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.entity.SubscriptionEntity;
import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.repository.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanFeatureResolverTest {

    @Mock
    private SubscriptionManager subscriptionManager;

    private PlanFeatureResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PlanFeatureResolver(subscriptionManager);
    }

    private UserEntity createUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@example.com");
        return user;
    }

    private SubscriptionEntity subscriptionWithMaxLocations(int maxLocations) {
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().maxLocations(maxLocations).build();
        return SubscriptionEntity.builder().subscriptionPlan(plan).build();
    }

    @Test
    void getMaxLocations_starterUser_shouldReturn1() {
        UserEntity user = createUser();
        when(subscriptionManager.getUserActiveSubscription(user)).thenReturn(Optional.of(subscriptionWithMaxLocations(1)));

        assertEquals(1, resolver.getMaxLocations(user));
    }

    @Test
    void getMaxLocations_professionalUser_shouldReturn999() {
        UserEntity user = createUser();
        when(subscriptionManager.getUserActiveSubscription(user)).thenReturn(Optional.of(subscriptionWithMaxLocations(999)));

        assertEquals(999, resolver.getMaxLocations(user));
    }

    @Test
    void getMaxLocations_noSubscription_shouldReturnDefaultOf1() {
        UserEntity user = createUser();
        when(subscriptionManager.getUserActiveSubscription(user)).thenReturn(Optional.empty());

        assertEquals(1, resolver.getMaxLocations(user));
    }

    @Test
    void getMaxLocations_withNullUser_shouldReturnDefaultOf1() {
        assertEquals(1, resolver.getMaxLocations(null));
    }

    @Test
    void filterLocationsByPlan_starterUser_shouldLimit1Location() {
        UserEntity user = createUser();
        when(subscriptionManager.getUserActiveSubscription(user)).thenReturn(Optional.of(subscriptionWithMaxLocations(1)));
        List<Map<String, Object>> locations = List.of(
                Map.of("name", "loc-1", "title", "Location 1"),
                Map.of("name", "loc-2", "title", "Location 2"),
                Map.of("name", "loc-3", "title", "Location 3")
        );

        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(user, locations);

        assertEquals(1, filtered.size());
        assertEquals("loc-1", filtered.get(0).get("name"));
    }

    @Test
    void filterLocationsByPlan_professionalUser_shouldReturnAllLocations() {
        UserEntity user = createUser();
        when(subscriptionManager.getUserActiveSubscription(user)).thenReturn(Optional.of(subscriptionWithMaxLocations(999)));
        List<Map<String, Object>> locations = List.of(
                Map.of("name", "loc-1", "title", "Location 1"),
                Map.of("name", "loc-2", "title", "Location 2"),
                Map.of("name", "loc-3", "title", "Location 3")
        );

        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(user, locations);

        assertEquals(3, filtered.size());
    }

    @Test
    void filterLocationsByPlan_withEmptyList_shouldReturnEmpty() {
        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(createUser(), List.of());

        assertTrue(filtered.isEmpty());
    }

    @Test
    void filterLocationsByPlan_withNullList_shouldReturnNull() {
        List<Map<String, Object>> filtered = resolver.filterLocationsByPlan(createUser(), null);

        assertNull(filtered);
    }
}
