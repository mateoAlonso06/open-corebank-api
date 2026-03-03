package com.banking.system.transaction.application.usecase;

import com.banking.system.account.application.dto.result.AccountLimitResult;

import java.util.UUID;

public interface GetAccountLimitsUseCase {
    AccountLimitResult getAccountLimits(UUID accountId, UUID userId);
}
