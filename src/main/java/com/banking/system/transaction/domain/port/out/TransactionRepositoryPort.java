package com.banking.system.transaction.domain.port.out;

import com.banking.system.common.domain.PageRequest;
import com.banking.system.common.domain.dto.PagedResult;
import com.banking.system.transaction.domain.model.Transaction;
import com.banking.system.transaction.domain.model.TransactionStatus;
import com.banking.system.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    Optional<Transaction> findById(UUID transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    PagedResult<Transaction> findAllTransactionsByAccountId(PageRequest pageRequest, UUID accountId);

    PagedResult<Transaction> findALlByAccountIds(List<UUID> accountIds, PageRequest pageRequest);

    PagedResult<Transaction> findAllByAccountIdsAndStatus(List<UUID> accountIds, TransactionStatus status, PageRequest pageRequest);

    BigDecimal sumCompletedAmountByAccountIdAndTypeSince(UUID accountId, TransactionType type, Instant since);
}
