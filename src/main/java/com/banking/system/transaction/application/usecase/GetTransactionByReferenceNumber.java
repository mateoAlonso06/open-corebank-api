package com.banking.system.transaction.application.usecase;

import com.banking.system.transaction.application.dto.result.TransactionResult;

import java.util.UUID;

public interface GetTransactionByReferenceNumber {
    TransactionResult getTransactionByReferenceNumber(String referenceNumber, UUID userId);
}
