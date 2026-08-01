package com.bchev.notezen.domain.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    @Value("${stripe.api-key}")
    private String apiKey;

    private RequestOptions getRequestOptions() {
        return RequestOptions.builder()
                .setApiKey(apiKey)
                .build();
    }

    public Customer createCustomer(String email, String name) throws StripeException {
        Stripe.apiKey = apiKey;

        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        if (name != null && !name.isEmpty()) {
            params.put("name", name);
        }

        Customer customer = Customer.create(params, getRequestOptions());
        log.info("Created Stripe customer {} for email {}", customer.getId(), email);
        return customer;
    }

    public Subscription getSubscription(String stripeSubscriptionId) throws StripeException {
        Stripe.apiKey = apiKey;
        Subscription subscription = Subscription.retrieve(stripeSubscriptionId, getRequestOptions());
        return subscription;
    }

    public String getSubscriptionStatus(String stripeSubscriptionId) throws StripeException {
        Subscription subscription = getSubscription(stripeSubscriptionId);
        return subscription.getStatus();
    }

    public Invoice getInvoice(String stripeInvoiceId) throws StripeException {
        Stripe.apiKey = apiKey;
        Invoice invoice = Invoice.retrieve(stripeInvoiceId, getRequestOptions());
        return invoice;
    }

    /**
     * Annule immédiatement, pas à la fin de la période en cours : un client qui
     * annule ne doit plus avoir accès tout de suite, pas continuer à profiter du
     * service jusqu'à l'échéance (source de confusion, cf. incident constaté).
     */
    public void cancelSubscription(String stripeSubscriptionId) throws StripeException {
        Stripe.apiKey = apiKey;

        Subscription subscription = Subscription.retrieve(stripeSubscriptionId, getRequestOptions());
        subscription.cancel(new HashMap<>(), getRequestOptions());
        log.info("Canceled Stripe subscription {} immediately", stripeSubscriptionId);
    }

    public String createCheckoutSession(String customerId, String priceId, Integer trialDays,
                                         String clientReferenceId, String successUrl,
                                         String cancelUrl) throws StripeException {
        Stripe.apiKey = apiKey;

        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("price", priceId);
        lineItem.put("quantity", 1L);

        Map<String, Object> params = new HashMap<>();
        params.put("mode", "subscription");
        params.put("customer", customerId);
        params.put("client_reference_id", clientReferenceId);
        params.put("line_items", java.util.List.of(lineItem));
        params.put("success_url", successUrl);
        params.put("cancel_url", cancelUrl);

        if (trialDays != null && trialDays > 0) {
            params.put("subscription_data", Map.of("trial_period_days", trialDays));
        }

        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params, getRequestOptions());
        log.info("Created Stripe checkout session {}", session.getId());
        return session.getUrl();
    }
}
