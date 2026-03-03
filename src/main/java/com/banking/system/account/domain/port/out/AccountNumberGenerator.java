package com.banking.system.account.domain.port.out;

import com.banking.system.account.domain.model.value_object.AccountNumber;
import com.banking.system.account.domain.model.enums.AccountType;

/**
 * Port for generating unique account numbers.
 * Implementation resides in infrastructure layer.
 */
public interface AccountNumberGenerator {
    /**
     * Generates a unique 22-digit account number for the given account type.
     * Format: TT + 18 UUID digits + 2-digit verifier
     */
    AccountNumber generate(AccountType accountType);
}