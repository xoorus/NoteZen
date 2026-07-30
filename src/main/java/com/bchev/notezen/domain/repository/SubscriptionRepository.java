package com.bchev.notezen.domain.repository;

import com.bchev.notezen.domain.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByUser(UserEntity user);
    Optional<SubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<SubscriptionEntity> findByUserAndStatus(UserEntity user, String status);
    boolean existsByUserAndStatusIn(UserEntity user, java.util.Collection<String> statuses);
}
