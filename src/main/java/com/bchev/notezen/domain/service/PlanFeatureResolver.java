package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlanFeatureResolver {

    private static final int DEFAULT_MAX_LOCATIONS = 1;
    private static final int UNLIMITED_LOCATIONS = 999;

    private final SubscriptionManager subscriptionManager;
    private final AccessControlService accessControlService;

    public int getMaxLocations(UserEntity user) {
        if (user == null) {
            return DEFAULT_MAX_LOCATIONS;
        }
        if (accessControlService.isAllowlisted(user.getEmail())) {
            return UNLIMITED_LOCATIONS;
        }
        return subscriptionManager.getUserActiveSubscription(user)
                .map(subscription -> subscription.getSubscriptionPlan().getMaxLocations())
                .orElse(DEFAULT_MAX_LOCATIONS);
    }

    public List<Map<String, Object>> filterLocationsByPlan(UserEntity user, List<Map<String, Object>> allLocations) {
        if (allLocations == null || allLocations.isEmpty()) {
            return allLocations;
        }

        int maxLocations = getMaxLocations(user);
        List<Map<String, Object>> filtered = allLocations.stream()
                .limit(maxLocations)
                .toList();

        log.info("[PlanFeatureResolver] {} locations shown out of {} total for user {} (max: {})",
                filtered.size(), allLocations.size(), user.getEmail(), maxLocations);

        return filtered;
    }
}
