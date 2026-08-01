package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.entity.InvoiceEntity;
import com.bchev.notezen.domain.entity.SubscriptionEntity;
import com.bchev.notezen.domain.entity.SubscriptionPlanEntity;
import com.bchev.notezen.domain.repository.InvoiceRepository;
import com.bchev.notezen.domain.repository.SubscriptionPlanRepository;
import com.bchev.notezen.domain.repository.SubscriptionRepository;
import com.bchev.notezen.domain.repository.UserEntity;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionManagerTest {

    @Mock
    private StripeService stripeService;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    private SubscriptionManager subscriptionManager;

    private UserEntity user;
    private SubscriptionPlanEntity plan;

    @BeforeEach
    void setUp() {
        subscriptionManager = new SubscriptionManager(
                stripeService, subscriptionRepository, subscriptionPlanRepository, invoiceRepository);

        user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@example.com");

        plan = SubscriptionPlanEntity.builder()
                .id(1L)
                .name("Pro")
                .stripePriceId("price_123")
                .trialDays(14)
                .build();
    }

    private Subscription stripeSubscription(String id, String status, Long trialEnd) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setStatus(status);
        subscription.setCurrentPeriodStart(1_700_000_000L);
        subscription.setCurrentPeriodEnd(1_702_600_000L);
        subscription.setTrialEnd(trialEnd);
        return subscription;
    }

    @Test
    void startCheckout_newUser_shouldCreateStripeCustomerAndReturnCheckoutUrl() throws StripeException {
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());

        Customer customer = new Customer();
        customer.setId("cus_new");
        when(stripeService.createCustomer("user@example.com", null)).thenReturn(customer);

        when(stripeService.createCheckoutSession("cus_new", "price_123", 14, "1",
                "http://localhost:4200/dashboard", "http://localhost:4200/dashboard"))
                .thenReturn("https://checkout.stripe.com/session_abc");

        String url = subscriptionManager.startCheckout(user,
                "http://localhost:4200/dashboard", "http://localhost:4200/dashboard");

        assertEquals("https://checkout.stripe.com/session_abc", url);
        verify(stripeService).createCustomer("user@example.com", null);
    }

    @Test
    void startCheckout_userWithExistingSubscription_shouldReuseStripeCustomerId() throws StripeException {
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.of(plan));
        SubscriptionEntity existing = SubscriptionEntity.builder()
                .stripeCustomerId("cus_existing")
                .status("past_due")
                .build();
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(existing));

        when(stripeService.createCheckoutSession(eq("cus_existing"), eq("price_123"), isNull(), eq("1"),
                eq("http://localhost:4200/dashboard"), eq("http://localhost:4200/dashboard")))
                .thenReturn("https://checkout.stripe.com/session_retry");

        String url = subscriptionManager.startCheckout(user,
                "http://localhost:4200/dashboard", "http://localhost:4200/dashboard");

        assertEquals("https://checkout.stripe.com/session_retry", url);
        verify(stripeService, never()).createCustomer(anyString(), any());
    }

    @Test
    void startCheckout_userWithCanceledSubscription_shouldNotGrantTrialAgain() throws StripeException {
        // Un utilisateur qui a déjà annulé un abonnement a déjà consommé son essai :
        // pas de nouveau trial, même s'il repasse par un nouveau checkout.
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.of(plan));
        SubscriptionEntity canceled = SubscriptionEntity.builder()
                .stripeCustomerId("cus_existing")
                .status("canceled")
                .build();
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(canceled));

        when(stripeService.createCheckoutSession(eq("cus_existing"), eq("price_123"), isNull(), eq("1"),
                anyString(), anyString()))
                .thenReturn("https://checkout.stripe.com/session_no_trial");

        String url = subscriptionManager.startCheckout(user, "url", "url");

        assertEquals("https://checkout.stripe.com/session_no_trial", url);
    }

    @Test
    void startCheckout_noActivePlan_shouldThrowIllegalStateException() {
        when(subscriptionPlanRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> subscriptionManager.startCheckout(user, "url", "url"));
    }

    @Test
    void persistSubscriptionFromCheckout_withTrial_shouldMapFieldsAndSave() {
        when(subscriptionRepository.save(any(SubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription stripeSub = stripeSubscription("sub_123", "trialing", 1_701_300_000L);

        SubscriptionEntity result = subscriptionManager.persistSubscriptionFromCheckout(user, plan, stripeSub, "cus_new");

        assertEquals("cus_new", result.getStripeCustomerId());
        assertEquals("sub_123", result.getStripeSubscriptionId());
        assertEquals("trialing", result.getStatus());
        assertNotNull(result.getTrialEndDate());
        verify(subscriptionRepository).save(result);
    }

    @Test
    void persistSubscriptionFromCheckout_withoutTrial_shouldLeaveTrialEndDateNull() {
        when(subscriptionRepository.save(any(SubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription stripeSub = stripeSubscription("sub_456", "active", null);

        SubscriptionEntity result = subscriptionManager.persistSubscriptionFromCheckout(user, plan, stripeSub, "cus_existing");

        assertEquals("cus_existing", result.getStripeCustomerId());
        assertEquals("active", result.getStatus());
        assertNull(result.getTrialEndDate());
    }

    @Test
    void persistSubscriptionFromCheckout_existingRow_shouldUpdateInPlaceInsteadOfInserting() {
        // Retry après un past_due (carte refusée puis nouvelle tentative réussie) :
        // la ligne existante doit être mise à jour, pas dupliquée (contrainte unique sur user_id).
        SubscriptionEntity existing = SubscriptionEntity.builder()
                .id(99L)
                .user(user)
                .stripeSubscriptionId("sub_old_failed")
                .stripeCustomerId("cus_existing")
                .status("past_due")
                .canceledAt(null)
                .build();
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(SubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription stripeSub = stripeSubscription("sub_new_retry", "active", null);

        SubscriptionEntity result = subscriptionManager.persistSubscriptionFromCheckout(user, plan, stripeSub, "cus_existing");

        assertEquals(99L, result.getId());
        assertEquals("sub_new_retry", result.getStripeSubscriptionId());
        assertEquals("active", result.getStatus());
        verify(subscriptionRepository).save(existing);
    }

    @Test
    void syncSubscriptionStatus_statusChanged_shouldUpdateAndSave() throws StripeException {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(1L)
                .stripeSubscriptionId("sub_123")
                .status("trialing")
                .build();
        when(stripeService.getSubscriptionStatus("sub_123")).thenReturn("active");

        subscriptionManager.syncSubscriptionStatus(subscription);

        assertEquals("active", subscription.getStatus());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void syncSubscriptionStatus_statusUnchanged_shouldNotSave() throws StripeException {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(1L)
                .stripeSubscriptionId("sub_123")
                .status("active")
                .build();
        when(stripeService.getSubscriptionStatus("sub_123")).thenReturn("active");

        subscriptionManager.syncSubscriptionStatus(subscription);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void handlePaymentSuccess_newInvoice_shouldRecordInvoiceAndActivateSubscription() throws StripeException {
        when(invoiceRepository.findByStripeInvoiceId("in_123")).thenReturn(Optional.empty());

        Invoice invoice = new Invoice();
        invoice.setId("in_123");
        invoice.setSubscription("sub_123");
        invoice.setAmountPaid(1990L);
        invoice.setCurrency("eur");
        invoice.setCreated(1_700_000_000L);
        when(stripeService.getInvoice("in_123")).thenReturn(invoice);

        SubscriptionEntity dbSubscription = SubscriptionEntity.builder()
                .stripeSubscriptionId("sub_123")
                .status("past_due")
                .user(user)
                .build();
        when(subscriptionRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(dbSubscription));

        subscriptionManager.handlePaymentSuccess("in_123");

        ArgumentCaptor<InvoiceEntity> invoiceCaptor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertEquals(0, new BigDecimal("19.90").compareTo(invoiceCaptor.getValue().getAmount()));
        assertEquals("paid", invoiceCaptor.getValue().getStatus());
        assertEquals("active", dbSubscription.getStatus());
        verify(subscriptionRepository).save(dbSubscription);
    }

    @Test
    void handlePaymentSuccess_alreadyProcessedInvoice_shouldSkip() throws StripeException {
        when(invoiceRepository.findByStripeInvoiceId("in_123"))
                .thenReturn(Optional.of(InvoiceEntity.builder().stripeInvoiceId("in_123").build()));

        subscriptionManager.handlePaymentSuccess("in_123");

        verify(stripeService, never()).getInvoice(anyString());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void handlePaymentFailure_newInvoice_shouldRecordFailedInvoiceAndMarkPastDue() throws StripeException {
        when(invoiceRepository.findByStripeInvoiceId("in_456")).thenReturn(Optional.empty());

        Invoice invoice = new Invoice();
        invoice.setId("in_456");
        invoice.setSubscription("sub_123");
        invoice.setAmountDue(1990L);
        invoice.setCurrency("eur");
        when(stripeService.getInvoice("in_456")).thenReturn(invoice);

        SubscriptionEntity dbSubscription = SubscriptionEntity.builder()
                .stripeSubscriptionId("sub_123")
                .status("active")
                .user(user)
                .build();
        when(subscriptionRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(dbSubscription));

        subscriptionManager.handlePaymentFailure("in_456", "Card declined");

        ArgumentCaptor<InvoiceEntity> invoiceCaptor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertEquals("failed", invoiceCaptor.getValue().getStatus());
        assertEquals("Card declined", invoiceCaptor.getValue().getFailureMessage());
        assertEquals("past_due", dbSubscription.getStatus());
    }

    @Test
    void getUserActiveSubscription_shouldReturnSubscriptionRegardlessOfStatus() {
        SubscriptionEntity trialing = SubscriptionEntity.builder().status("trialing").build();
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.of(trialing));

        Optional<SubscriptionEntity> result = subscriptionManager.getUserActiveSubscription(user);

        assertTrue(result.isPresent());
        assertEquals("trialing", result.get().getStatus());
    }

    @Test
    void hasActiveSubscription_shouldDelegateToRepositoryWithActiveAndTrialingStatuses() {
        when(subscriptionRepository.existsByUserAndStatusIn(eq(user), any())).thenReturn(true);

        assertTrue(subscriptionManager.hasActiveSubscription(user));
    }
}
