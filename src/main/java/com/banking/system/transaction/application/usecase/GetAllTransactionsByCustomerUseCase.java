package com.banking.system.transaction.application.usecase;

import com.banking.system.common.domain.PageRequest;
import com.banking.system.common.domain.dto.PagedResult;
import com.banking.system.transaction.application.dto.result.TransactionResult;

import java.util.UUID;

public interface GetAllTransactionsByCustomerUseCase {
    PagedResult<TransactionResult> getAllTransactionsByCustomer(UUID userId, boolean typeTransfer, PageRequest pageRequest);
}