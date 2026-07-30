package com.bchev.notezen.domain.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.bchev.notezen.domain.entity.SubscriptionEntity;
import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.entity.InvoiceEntity;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import com.bchev.notezen.domain.repository.SubscriptionRepository;
import com.bchev.notezen.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionManager {

    private final StripeService stripeService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Démarre un Checkout Stripe pour un utilisateur non (ou plus) autorisé.
     * Réutilise le Customer Stripe existant si l'utilisateur a déjà une
     * subscription en base (ex: paiement précédent échoué), pour éviter de
     * dupliquer des Customers Stripe à chaque tentative.
     */
    public String startCheckout(UserEntity user, String successUrl, String cancelUrl) throws StripeException {
        SubscriptionPlanEntity plan = subscriptionPlanRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Aucun plan d'abonnement actif configuré"));

        Optional<SubscriptionEntity> existing = subscriptionRepository.findByUser(user);
        String stripeCustomerId = existing.isPresent()
                ? existing.get().getStripeCustomerId()
                : stripeService.createCustomer(user.getEmail(), null).getId();

        return stripeService.createCheckoutSession(
                stripeCustomerId,
                plan.getStripePriceId(),
                plan.getTrialDays(),
                String.valueOf(user.getId()),
                successUrl,
                cancelUrl
        );
    }

    /**
     * Persiste la subscription en DB une fois le paiement confirmé côté Stripe
     * (déclenché par le webhook checkout.session.completed). On ne crée jamais
     * de SubscriptionEntity avant ce point : tant qu'aucun moyen de paiement
     * n'est attaché, il n'y a rien à persister côté métier.
     */
    @Transactional
    public SubscriptionEntity persistSubscriptionFromCheckout(UserEntity user, SubscriptionPlanEntity plan,
                                                                Subscription stripeSubscription,
                                                                String stripeCustomerId) {
        LocalDateTime currentPeriodStart = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()),
                ZoneId.systemDefault()
        );
        LocalDateTime currentPeriodEnd = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()),
                ZoneId.systemDefault()
        );

        LocalDateTime trialEndDate = null;
        if (stripeSubscription.getTrialEnd() != null) {
            trialEndDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeSubscription.getTrialEnd()),
                    ZoneId.systemDefault()
            );
        }

        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .user(user)
                .subscriptionPlan(plan)
                .stripeSubscriptionId(stripeSubscription.getId())
                .stripeCustomerId(stripeCustomerId)
                .status(stripeSubscription.getStatus())
                .currentPeriodStart(currentPeriodStart)
                .currentPeriodEnd(currentPeriodEnd)
                .trialEndDate(trialEndDate)
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("Persisted subscription {} for user {} with plan {}",
                subscription.getStripeSubscriptionId(), user.getId(), plan.getId());
        return subscription;
    }

    @Transactional
    public void syncSubscriptionStatus(SubscriptionEntity subscription) throws StripeException {
        String status = stripeService.getSubscriptionStatus(subscription.getStripeSubscriptionId());

        if (!status.equals(subscription.getStatus())) {
            subscription.setStatus(status);
            subscriptionRepository.save(subscription);
            log.info("Synced subscription {} status to {}", subscription.getId(), status);
        }
    }

    @Transactional
    public void handlePaymentSuccess(String stripeInvoiceId) throws StripeException {
        Optional<InvoiceEntity> existingInvoice = invoiceRepository.findByStripeInvoiceId(stripeInvoiceId);
        if (existingInvoice.isPresent()) {
            log.info("Invoice {} already processed", stripeInvoiceId);
            return;
        }

        Invoice stripeInvoice = stripeService.getInvoice(stripeInvoiceId);
        String subscriptionId = stripeInvoice.getSubscription();

        Optional<SubscriptionEntity> subscription = subscriptionRepository
                .findByStripeSubscriptionId(subscriptionId);

        if (subscription.isPresent()) {
            LocalDateTime paidAt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeInvoice.getCreated()),
                    ZoneId.systemDefault()
            );

            InvoiceEntity invoice = InvoiceEntity.builder()
                    .user(subscription.get().getUser())
                    .subscription(subscription.get())
                    .stripeInvoiceId(stripeInvoiceId)
                    .amount(new BigDecimal(stripeInvoice.getAmountPaid()).divide(new BigDecimal(100)))
                    .currency(stripeInvoice.getCurrency())
                    .status("paid")
                    .paidAt(paidAt)
                    .build();

            invoiceRepository.save(invoice);

            // Mettre à jour subscription status
            subscription.get().setStatus("active");
            subscriptionRepository.save(subscription.get());

            log.info("Payment success for invoice {} subscription {}", stripeInvoiceId, subscriptionId);
        }
    }

    @Transactional
    public void handlePaymentFailure(String stripeInvoiceId, String failureReason) throws StripeException {
        Optional<InvoiceEntity> existingInvoice = invoiceRepository.findByStripeInvoiceId(stripeInvoiceId);
        if (existingInvoice.isPresent()) {
            log.info("Invoice {} already processed", stripeInvoiceId);
            return;
        }

        Invoice stripeInvoice = stripeService.getInvoice(stripeInvoiceId);
        String subscriptionId = stripeInvoice.getSubscription();

        Optional<SubscriptionEntity> subscription = subscriptionRepository
                .findByStripeSubscriptionId(subscriptionId);

        if (subscription.isPresent()) {
            InvoiceEntity invoice = InvoiceEntity.builder()
                    .user(subscription.get().getUser())
                    .subscription(subscription.get())
                    .stripeInvoiceId(stripeInvoiceId)
                    .amount(new BigDecimal(stripeInvoice.getAmountDue()).divide(new BigDecimal(100)))
                    .currency(stripeInvoice.getCurrency())
                    .status("failed")
                    .failureMessage(failureReason)
                    .nextRetryAt(LocalDateTime.now().plusDays(3))
                    .build();

            invoiceRepository.save(invoice);

            // Mettre à jour subscription status
            subscription.get().setStatus("past_due");
            subscriptionRepository.save(subscription.get());

            log.warn("Payment failure for invoice {} subscription {}: {}",
                    stripeInvoiceId, subscriptionId, failureReason);
        }
    }

    public Optional<SubscriptionEntity> getUserActiveSubscription(UserEntity user) {
        return subscriptionRepository.findByUser(user);
    }

    public boolean hasActiveSubscription(UserEntity user) {
        return subscriptionRepository.existsByUserAndStatusIn(user, java.util.Arrays.asList("active", "trialing"));
    }
}
