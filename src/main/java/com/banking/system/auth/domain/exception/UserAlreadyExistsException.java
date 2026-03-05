package com.banking.system.auth.domain.exception;

import com.banking.system.common.domain.exception.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String message) {
        super(message, "EMAIL_ALREADY_IN_USE");
    }
}
