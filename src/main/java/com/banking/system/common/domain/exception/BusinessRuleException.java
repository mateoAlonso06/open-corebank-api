package com.banking.system.common.domain.exception;

/**
 * Base exception for business rule violations.
 * Use when a domain invariant or business constraint is violated.
 * Maps to HTTP 422 Unprocessable Entity or 400 Bad Request.
 *
 * clientMessage: the message safe to expose to the client in production.
 * - 1-arg and 2-arg constructors: clientMessage = message (caller is responsible for writing a user-facing string)
 * - 3-arg constructor: explicit clientMessage for when the internal message contains sensitive data
 *   (e.g. account IDs, exact amounts) that should not be leaked to the client.
 */
public abstract class BusinessRuleException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    private final String clientMessage;

    protected BusinessRuleException(String message) {
        super(message, DEFAULT_ERROR_CODE);
        this.clientMessage = message;
    }

    protected BusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
        this.clientMessage = message;
    }

    protected BusinessRuleException(String message, String errorCode, String clientMessage) {
        super(message, errorCode);
        this.clientMessage = clientMessage;
    }

    public String getClientMessage() {
        return clientMessage;
    }
}
