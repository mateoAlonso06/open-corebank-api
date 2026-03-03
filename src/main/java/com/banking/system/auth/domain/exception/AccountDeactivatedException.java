package com.banking.system.auth.domain.exception;

import com.banking.system.common.domain.exception.DomainException;

public class AccountDeactivatedException extends DomainException {
    public AccountDeactivatedException(String message) {
        super(message, "ACCOUNT_DEACTIVATED");
    }
}
