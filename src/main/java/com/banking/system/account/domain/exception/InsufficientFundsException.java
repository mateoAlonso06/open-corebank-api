package com.banking.system.account.domain.exception;

import com.banking.system.common.domain.Money;
import com.banking.system.common.domain.exception.BusinessRuleException;

import java.util.UUID;

public class InsufficientFundsException extends BusinessRuleException {
    public InsufficientFundsException(UUID accountId, Money requested, Money available) {
        super(
                String.format(
                        "Insufficient funds in account %s: requested %s but only %s available",
                        accountId, requested, available
                ),
                "INSUFFICIENT_FUNDS",
                "Insufficient funds"
        );
    }
}
