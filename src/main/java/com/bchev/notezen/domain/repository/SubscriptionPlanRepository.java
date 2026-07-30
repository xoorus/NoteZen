package com.bchev.notezen.domain.repository;

import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, Long> {
    Optional<SubscriptionPlanEntity> findByStripePriceId(String stripePriceId);
    Optional<SubscriptionPlanEntity> findByStripeProductId(String stripeProductId);
    Optional<SubscriptionPlanEntity> findByActiveTrue();
}
