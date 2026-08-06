package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.model.PricingPlan;
import com.bchev.notezen.domain.repository.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PlanFeatureResolver {

    public int getMaxLocations(UserEntity user) {
        if (user == null || user.getPricingPlan() == null) {
            return PricingPlan.STARTER.getMaxLocations();
        }
        return user.getPricingPlan().getMaxLocations();
    }

    public List<Map<String, Object>> filterLocationsByPlan(
            UserEntity user,
            List<Map<String, Object>> allLocations) {
        if (allLocations == null || allLocations.isEmpty()) {
            return allLocations;
        }

        int maxLocations = getMaxLocations(user);
        log.info("[PlanFeatureResolver] Filtering locations for user {} (plan: {}, max: {})",
                user.getEmail(), user.getPricingPlan(), maxLocations);

        List<Map<String, Object>> filtered = allLocations.stream()
                .limit(maxLocations)
                .toList();

        log.info("[PlanFeatureResolver] {} locations shown out of {} total",
                filtered.size(), allLocations.size());

        return filtered;
    }

    public boolean canAccessLocation(UserEntity user, int locationIndex) {
        return locationIndex < getMaxLocations(user);
    }

    public String getPlanName(UserEntity user) {
        return user.getPricingPlan() != null
            ? user.getPricingPlan().name()
            : PricingPlan.STARTER.name();
    }

    public double getMonthlyPrice(UserEntity user) {
        return user.getPricingPlan() != null
            ? user.getPricingPlan().getMonthlyPrice()
            : PricingPlan.STARTER.getMonthlyPrice();
    }
}
