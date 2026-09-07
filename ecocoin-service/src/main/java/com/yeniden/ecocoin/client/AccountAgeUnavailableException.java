package com.yeniden.ecocoin.client;

/** Signals a transient eligibility lookup failure so AMQP can retry the handover message. */
public class AccountAgeUnavailableException extends RuntimeException {
    public AccountAgeUnavailableException(java.util.UUID userId) {
        super("Trusted account creation time is currently unavailable for user " + userId);
    }
}
