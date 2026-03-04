package com.banking.system.transaction.infraestructure.adapter.out.persistence.repository;

import com.banking.system.common.domain.PageRequest;
import com.banking.system.common.domain.dto.PagedResult;
import com.banking.system.common.infraestructure.mapper.PageMapper;
import com.banking.system.transaction.domain.model.Transaction;
import com.banking.system.transaction.domain.model.enums.TransactionStatus;
import com.banking.system.transaction.domain.model.enums.TransactionType;
import com.banking.system.transaction.domain.port.out.TransactionRepositoryPort;
import com.banking.system.transaction.infraestructure.adapter.out.mapper.TransactionJpaEntityMapper;
import com.banking.system.transaction.infraestructure.adapter.out.persistence.entity.TransactionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {
    private final SpringDataTransactionRepository transactionJpaRepository;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity txJpaEntity = TransactionJpaEntity.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccountId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount().getValue())
                .currency(transaction.getAmount().getCurrency().code())
                .balanceAfter(transaction.getBalanceAfter().getValue())
                .description(transaction.getDescription().value())
                .referenceNumber(transaction.getReferenceNumber().value())
                .status(transaction.getStatus())
                .idempotencyKey(transaction.getIdempotencyKey() != null ? transaction.getIdempotencyKey().value() : null)
                .executedAt(transaction.getExecutedAt())
                .build();

        TransactionJpaEntity txJpaEntitySaved = transactionJpaRepository.save(txJpaEntity);

        return TransactionJpaEntityMapper.toDomainEntity(txJpaEntitySaved);
    }

    @Override
    public Optional<Transaction> findByReferenceNumber(String referenceNumber) {
        var transactionJpaEntity = transactionJpaRepository.findByReferenceNumber(referenceNumber);
        return transactionJpaEntity
                .map(TransactionJpaEntityMapper::toDomainEntity);
    }

    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        var transactionJpaEntity = transactionJpaRepository.findById(transactionId);
        return transactionJpaEntity
                .map(TransactionJpaEntityMapper::toDomainEntity);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return transactionJpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransactionJpaEntityMapper::toDomainEntity);
    }

    private static final Sort SORT_BY_EXECUTED_AT_DESC = Sort.by(Sort.Direction.DESC, "executedAt");

    @Override
    public PagedResult<Transaction> findAllTransactionsByAccountId(PageRequest pageRequest, UUID accountId) {
        var base = PageMapper.toPageable(pageRequest);
        var pageable = org.springframework.data.domain.PageRequest.of(
                base.getPageNumber(),
                base.getPageSize(),
                SORT_BY_EXECUTED_AT_DESC
        );

        var page = transactionJpaRepository.findAllByAccountId(accountId, pageable);
        return PageMapper.toPagedResult(page, TransactionJpaEntityMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Transaction> findALlByAccountIds(List<UUID> accountIds, PageRequest pageRequest) {
        var base = PageMapper.toPageable(pageRequest);
        var pageable = org.springframework.data.domain.PageRequest.of(
                base.getPageNumber(),
                base.getPageSize(),
                SORT_BY_EXECUTED_AT_DESC
        );

        var page = transactionJpaRepository.findAllByAccountIdIn(accountIds, pageable);

        return PageMapper.toPagedResult(page, TransactionJpaEntityMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Transaction> findAllByAccountIdsAndStatus(List<UUID> accountIds, TransactionStatus status, PageRequest pageRequest) {
        var base = PageMapper.toPageable(pageRequest);
        var pageable = org.springframework.data.domain.PageRequest.of(
                base.getPageNumber(),
                base.getPageSize(),
                SORT_BY_EXECUTED_AT_DESC
        );

        var page = transactionJpaRepository.findAllByAccountIdInAndStatus(accountIds, status, pageable);

        return PageMapper.toPagedResult(page, TransactionJpaEntityMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Transaction> findAllByAccountIdsAndType(List<UUID> accountIds, String typeTransfer, PageRequest pageRequest) {
        var base = PageMapper.toPageable(pageRequest);
        var pageable = org.springframework.data.domain.PageRequest.of(
                base.getPageNumber(),
                base.getPageSize(),
                SORT_BY_EXECUTED_AT_DESC
        );
        var page = transactionJpaRepository.findAllByAccountIdAndTransactionType(accountIds, typeTransfer, pageable);
        return PageMapper.toPagedResult(page, TransactionJpaEntityMapper::toDomainEntity);
    }

    @Override
    public BigDecimal sumCompletedAmountByAccountIdAndTypeSince(UUID accountId, TransactionType type, Instant since) {
        return transactionJpaRepository.sumCompletedAmountByAccountIdAndTypeSince(accountId, type, since);
    }
}
