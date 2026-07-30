package com.bchev.notezen.domain.exception;

public class SubscriptionCanceledException extends RuntimeException {
    public SubscriptionCanceledException(String message) {
        super(message);
    }

    public SubscriptionCanceledException(String message, Throwable cause) {
        super(message, cause);
    }
}
