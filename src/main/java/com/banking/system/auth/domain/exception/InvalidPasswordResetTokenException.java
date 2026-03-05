package com.banking.system.auth.domain.exception;

import com.banking.system.common.domain.exception.BusinessRuleException;

public class InvalidPasswordResetTokenException extends BusinessRuleException {
    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}
