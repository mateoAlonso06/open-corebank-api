package com.banking.system.transaction.application.dto.command;

import com.banking.system.transaction.domain.model.enums.TransferCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyCommand(
        UUID fromAccountId,
        String toAlias,
        String toAccountNumber,
        BigDecimal amount,
        String currency,
        BigDecimal feeAmount,
        TransferCategory category,
        String feeCurrency,
        String description,
        UUID idempotencyKey
) {
}
