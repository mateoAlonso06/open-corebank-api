package com.banking.system.account.domain.exception;

import com.banking.system.common.domain.exception.BusinessRuleException;

public class AccountHasNonZeroBalanceException extends BusinessRuleException {
    public AccountHasNonZeroBalanceException(String message) {
        super(message, "ACCOUNT_HAS_NON_ZERO_BALANCE",
                "One or more accounts still have a balance. Please withdraw or transfer your funds before deactivating your account.");
    }
}