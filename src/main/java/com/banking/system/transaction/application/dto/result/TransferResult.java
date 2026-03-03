package com.banking.system.transaction.application.dto.result;

import com.banking.system.transaction.domain.model.enums.TransferCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResult(
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        TransferCategory category,
        BigDecimal amount,
        UUID creditTransactionId,
        UUID debitTransactionId,
        UUID feeTransactionId
) {
}
