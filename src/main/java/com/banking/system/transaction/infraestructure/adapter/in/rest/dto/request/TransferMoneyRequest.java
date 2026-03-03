package com.banking.system.transaction.infraestructure.adapter.in.rest.dto.request;

import com.banking.system.common.infraestructure.utils.SanitizeHtml;
import com.banking.system.transaction.application.dto.command.TransferMoneyCommand;
import com.banking.system.transaction.domain.model.enums.TransferCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyRequest(
        @NotNull(message = "Source account ID is required")
        UUID fromAccountId,

        @Pattern(regexp = "^[a-z0-9._-]{6,20}$", message = "Alias must be 6-20 characters long and contain only lowercase letters, digits, dots, hyphens, or underscores")
        @SanitizeHtml
        String toAlias,

        @Pattern(regexp = "^\\d{22}$", message = "Account number must be exactly 22 digits")
        @SanitizeHtml
        String toAccountNumber,

        @Positive
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 2, message = "Amount must have a maximum of 19 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g., USD, EUR)")
        @SanitizeHtml
        String currency,

        @Positive
        @DecimalMin(value = "0.00", message = "Fee amount cannot be negative")
        @Digits(integer = 15, fraction = 2, message = "Fee amount must have a maximum of 19 integer digits and 2 decimal places")
        BigDecimal feeAmount,

        @Size(min = 3, max = 3, message = "Fee currency must be a 3-letter ISO code (e.g., ARS, USD)")
        String feeCurrency,

        TransferCategory category,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        @SanitizeHtml
        @NotBlank
        String description,

        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey
) {

    public TransferMoneyCommand toCommand() {
        boolean hasAlias = toAlias != null && !toAlias.isBlank();
        boolean hasAccountNumber = toAccountNumber != null && !toAccountNumber.isBlank();

        if (hasAlias == hasAccountNumber) {
            throw new IllegalArgumentException("Exactly one of 'toAlias' or 'toAccountNumber' must be provided");
        }

        return new TransferMoneyCommand(
                fromAccountId,
                hasAlias ? toAlias : null,
                hasAccountNumber ? toAccountNumber : null,
                amount,
                currency,
                feeAmount,
                category,
                feeCurrency,
                description,
                idempotencyKey
        );
    }
}
