package com.banking.system.auth.domain.exception;

import com.banking.system.common.domain.exception.DomainException;

public class PasswordValidationException extends DomainException {

    public PasswordValidationException(String message) {
        super(message, "PASSWORD_VALIDATION_FAILED");
    }
}