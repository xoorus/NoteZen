package com.bchev.notezen.domain.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    @Value("${stripe.api-key}")
    private String apiKey;

    public StripeService() {
    }

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

    public Subscription createSubscription(String customerId, String priceId, Integer trialDays)
            throws StripeException {
        Stripe.apiKey = apiKey;

        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                        SubscriptionCreateParams.Item.builder()
                                .setPrice(priceId)
                                .build()
                );

        if (trialDays != null && trialDays > 0) {
            paramsBuilder.setTrialPeriodDays(Long.valueOf(trialDays));
        }

        Subscription subscription = Subscription.create(paramsBuilder.build(), getRequestOptions());
        log.info("Created Stripe subscription {} for customer {}", subscription.getId(), customerId);
        return subscription;
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

    public String constructWebhookEvent(String payload, String signature) throws StripeException {
        Stripe.apiKey = apiKey;

        String endpointSecret = apiKey;
        Event event = Webhook.constructEvent(
                payload, signature, endpointSecret);

        return event.getType();
    }

    public void cancelSubscription(String stripeSubscriptionId) throws StripeException {
        Stripe.apiKey = apiKey;

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();

        Subscription subscription = Subscription.retrieve(stripeSubscriptionId, getRequestOptions());
        subscription.update(params, getRequestOptions());
        log.info("Canceled Stripe subscription {}", stripeSubscriptionId);
    }

    public String createCheckoutSession(String customerId, String priceId, String successUrl,
                                       String cancelUrl) throws StripeException {
        Stripe.apiKey = apiKey;

        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("price", priceId);
        lineItem.put("quantity", 1L);

        Map<String, Object> params = new HashMap<>();
        params.put("mode", "subscription");
        params.put("customer", customerId);
        params.put("line_items", java.util.List.of(lineItem));
        params.put("success_url", successUrl);
        params.put("cancel_url", cancelUrl);

        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params, getRequestOptions());
        log.info("Created Stripe checkout session {}", session.getId());
        return session.getUrl();
    }
}
