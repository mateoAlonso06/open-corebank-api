package com.banking.system.transaction.application.dto.receipt;

import com.banking.system.transaction.domain.model.enums.TransferCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Receipt returned after executing a transfer operation.
 * Optimized for displaying transfer confirmation/voucher to the user.
 */
public record TransferReceipt(
        UUID transferId,
        String referenceNumber,
        TransferCategory category,
        BigDecimal amount,
        String currency,
        BigDecimal fee,
        String description,
        Instant executedAt,
        AccountInfo source,
        AccountInfo destination
) {
}
