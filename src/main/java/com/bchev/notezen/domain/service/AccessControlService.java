package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.exception.PaymentFailedException;
import com.bchev.notezen.domain.exception.SubscriptionCanceledException;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;
    private final SubscriptionManager subscriptionManager;

    private final List<String> authorizedEmails = List.of(
            "admin@notezen.fr",
            "dev@notezen.fr",
            "bchevriaut@gmail.com",
            "vanillelina33@gmail.com"
    );

    public boolean isAuthorized(String email) {
        String normalizedEmail = email.toLowerCase();

        // Allowlist toujours gratuit
        if (authorizedEmails.contains(normalizedEmail)) {
            log.debug("User {} authorized via allowlist", normalizedEmail);
            return true;
        }

        // Vérifier subscription pour les autres utilisateurs
        Optional<UserEntity> user = userRepository.findByEmail(normalizedEmail);

        if (user.isEmpty()) {
            log.warn("[Security] User not found: {}", normalizedEmail);
            return false;
        }

        // Vérifier si subscription existe et est valide
        Optional<com.bchev.notezen.domain.entity.SubscriptionEntity> subscription =
                subscriptionManager.getUserActiveSubscription(user.get());

        if (subscription.isEmpty()) {
            log.warn("[Security] No active subscription for user: {}", normalizedEmail);
            return false;
        }

        String status = subscription.get().getStatus();

        // Blocage si paiement échoué
        if ("past_due".equals(status)) {
            log.warn("[Security] User {} has failed payment", normalizedEmail);
            throw new PaymentFailedException("Paiement échoué. Veuillez mettre à jour vos informations de paiement.");
        }

        // Blocage si subscription annulée
        if ("canceled".equals(status)) {
            log.warn("[Security] User {} subscription is canceled", normalizedEmail);
            throw new SubscriptionCanceledException("Abonnement annulé. Veuillez vous réabonner pour continuer.");
        }

        // Accès si active ou trialing
        if ("active".equals(status) || "trialing".equals(status)) {
            log.debug("User {} authorized via subscription ({})", normalizedEmail, status);
            return true;
        }

        log.warn("[Security] User {} has invalid subscription status: {}", normalizedEmail, status);
        return false;
    }
}