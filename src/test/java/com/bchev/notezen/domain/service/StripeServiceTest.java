package com.bchev.notezen.domain.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StripeServiceTest {

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "apiKey", "sk_test_dummy");
    }

    @Test
    void createCustomer_withName_shouldIncludeNameInParams() throws StripeException {
        try (MockedStatic<Customer> mocked = mockStatic(Customer.class)) {
            Customer created = new Customer();
            created.setId("cus_123");
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            mocked.when(() -> Customer.create(paramsCaptor.capture(), any(RequestOptions.class)))
                    .thenReturn(created);

            Customer result = stripeService.createCustomer("user@example.com", "John Doe");

            assertEquals("cus_123", result.getId());
            assertEquals("user@example.com", paramsCaptor.getValue().get("email"));
            assertEquals("John Doe", paramsCaptor.getValue().get("name"));
        }
    }

    @Test
    void createCustomer_withNullName_shouldOmitNameParam() throws StripeException {
        try (MockedStatic<Customer> mocked = mockStatic(Customer.class)) {
            Customer created = new Customer();
            created.setId("cus_456");
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            mocked.when(() -> Customer.create(paramsCaptor.capture(), any(RequestOptions.class)))
                    .thenReturn(created);

            stripeService.createCustomer("user@example.com", null);

            assertFalse(paramsCaptor.getValue().containsKey("name"));
        }
    }

    @Test
    void getSubscription_shouldRetrieveByStripeId() throws StripeException {
        try (MockedStatic<Subscription> mocked = mockStatic(Subscription.class)) {
            Subscription sub = new Subscription();
            sub.setId("sub_123");
            mocked.when(() -> Subscription.retrieve(eq("sub_123"), any(RequestOptions.class)))
                    .thenReturn(sub);

            Subscription result = stripeService.getSubscription("sub_123");

            assertEquals("sub_123", result.getId());
        }
    }

    @Test
    void getSubscriptionStatus_shouldReturnStatusFromRetrievedSubscription() throws StripeException {
        try (MockedStatic<Subscription> mocked = mockStatic(Subscription.class)) {
            Subscription sub = new Subscription();
            sub.setId("sub_123");
            sub.setStatus("trialing");
            mocked.when(() -> Subscription.retrieve(eq("sub_123"), any(RequestOptions.class)))
                    .thenReturn(sub);

            String status = stripeService.getSubscriptionStatus("sub_123");

            assertEquals("trialing", status);
        }
    }

    @Test
    void getInvoice_shouldRetrieveByStripeId() throws StripeException {
        try (MockedStatic<Invoice> mocked = mockStatic(Invoice.class)) {
            Invoice invoice = new Invoice();
            invoice.setId("in_123");
            mocked.when(() -> Invoice.retrieve(eq("in_123"), any(RequestOptions.class)))
                    .thenReturn(invoice);

            Invoice result = stripeService.getInvoice("in_123");

            assertEquals("in_123", result.getId());
        }
    }

    @Test
    void cancelSubscription_shouldRetrieveThenCancelImmediately() throws StripeException {
        try (MockedStatic<Subscription> mocked = mockStatic(Subscription.class)) {
            Subscription sub = mock(Subscription.class);
            mocked.when(() -> Subscription.retrieve(eq("sub_123"), any(RequestOptions.class)))
                    .thenReturn(sub);

            stripeService.cancelSubscription("sub_123");

            verify(sub).cancel(anyMap(), any(RequestOptions.class));
        }
    }

    @Test
    void createCheckoutSession_withTrialDays_shouldIncludeSubscriptionData() throws StripeException {
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_123");
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/cs_123");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            mocked.when(() -> Session.create(paramsCaptor.capture(), any(RequestOptions.class)))
                    .thenReturn(session);

            String url = stripeService.createCheckoutSession(
                    "cus_123", "price_123", 14, "42",
                    "https://app/success", "https://app/cancel");

            assertEquals("https://checkout.stripe.com/cs_123", url);
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("subscription", params.get("mode"));
            assertEquals("cus_123", params.get("customer"));
            assertEquals("42", params.get("client_reference_id"));
            assertEquals("https://app/success", params.get("success_url"));
            assertEquals("https://app/cancel", params.get("cancel_url"));
            assertEquals(Map.of("trial_period_days", 14), params.get("subscription_data"));
            assertEquals(1, ((List<?>) params.get("line_items")).size());
        }
    }

    @Test
    void createCheckoutSession_withoutTrialDays_shouldOmitSubscriptionData() throws StripeException {
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            Session session = mock(Session.class);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/cs_456");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            mocked.when(() -> Session.create(paramsCaptor.capture(), any(RequestOptions.class)))
                    .thenReturn(session);

            stripeService.createCheckoutSession(
                    "cus_123", "price_123", null, "42",
                    "https://app/success", "https://app/cancel");

            assertFalse(paramsCaptor.getValue().containsKey("subscription_data"));
        }
    }

    @Test
    void createCheckoutSession_withZeroTrialDays_shouldOmitSubscriptionData() throws StripeException {
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            Session session = mock(Session.class);
            when(session.getUrl()).thenReturn("https://checkout.stripe.com/cs_789");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            mocked.when(() -> Session.create(paramsCaptor.capture(), any(RequestOptions.class)))
                    .thenReturn(session);

            stripeService.createCheckoutSession(
                    "cus_123", "price_123", 0, "42",
                    "https://app/success", "https://app/cancel");

            assertFalse(paramsCaptor.getValue().containsKey("subscription_data"));
        }
    }
}
