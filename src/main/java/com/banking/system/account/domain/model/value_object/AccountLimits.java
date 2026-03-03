package com.banking.system.account.domain.model.value_object;

import com.banking.system.account.domain.model.enums.AccountType;

import java.math.BigDecimal;

public record AccountLimits(
        BigDecimal dailyDepositLimit,
        BigDecimal monthlyDepositLimit,
        BigDecimal dailyWithdrawalLimit,
        BigDecimal monthlyWithdrawalLimit,
        BigDecimal dailyTransferLimit,
        BigDecimal monthlyTransferLimit
) {
    public static AccountLimits forType(AccountType type) {
        return switch (type) {
            case SAVINGS -> new AccountLimits(
                    new BigDecimal("500000.00"),
                    new BigDecimal("5000000.00"),
                    new BigDecimal("200000.00"),
                    new BigDecimal("2000000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("50000.00")
            );
            case CHECKING -> new AccountLimits(
                    new BigDecimal("1000000.00"),
                    new BigDecimal("10000000.00"),
                    new BigDecimal("500000.00"),
                    new BigDecimal("5000000.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("200000.00")
            );
            case INVESTMENT -> new AccountLimits(
                    new BigDecimal("2000000.00"),
                    new BigDecimal("20000000.00"),
                    new BigDecimal("1000000.00"),
                    new BigDecimal("10000000.00"),
                    new BigDecimal("100000.00"),
                    new BigDecimal("500000.00")
            );
        };
    }
}
