package com.banking.system.account.application.dto.command;

import com.banking.system.account.domain.model.enums.AccountType;

public record CreateAccountCommand(
    AccountType accountType,
    String currency
) {
}
