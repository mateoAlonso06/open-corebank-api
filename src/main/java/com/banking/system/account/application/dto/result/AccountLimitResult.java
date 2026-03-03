package com.banking.system.account.application.dto.result;

import com.banking.system.account.domain.model.Account;
import com.banking.system.account.domain.model.AccountLimits;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountLimitResult(
        UUID accountId,
        String accountType,
        LimitCategoryResult deposit,
        LimitCategoryResult withdrawal
) {
    public record LimitCategoryResult(
            LimitStatus daily,
            LimitStatus monthly
    ) {
    }

    public record LimitStatus(
            BigDecimal limit,
            BigDecimal used,
            BigDecimal remaining
    ) {
    }

    public static AccountLimitResult of(Account account, AccountLimits limits,
                                        BigDecimal depositToday, BigDecimal depositMonth,
                                        BigDecimal withdrawalToday, BigDecimal withdrawalMonth) {
        return new AccountLimitResult(
                account.getId(),
                account.getAccountType().name(),
                new LimitCategoryResult(
                        new LimitStatus(limits.dailyDepositLimit(), depositToday, limits.dailyDepositLimit().subtract(depositToday)),
                        new LimitStatus(limits.monthlyDepositLimit(), depositMonth, limits.monthlyDepositLimit().subtract(depositMonth))
                ),
                new LimitCategoryResult(
                        new LimitStatus(limits.dailyWithdrawalLimit(), withdrawalToday, limits.dailyWithdrawalLimit().subtract(withdrawalToday)),
                        new LimitStatus(limits.monthlyWithdrawalLimit(), withdrawalMonth, limits.monthlyWithdrawalLimit().subtract(withdrawalMonth))
                )
        );
    }
}
