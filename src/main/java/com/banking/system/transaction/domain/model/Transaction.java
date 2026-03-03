package com.banking.system.transaction.domain.model;

import com.banking.system.common.domain.Money;
import com.banking.system.transaction.domain.model.enums.TransactionStatus;
import com.banking.system.transaction.domain.model.enums.TransactionType;
import com.banking.system.transaction.domain.model.value_object.Description;
import com.banking.system.transaction.domain.model.value_object.IdempotencyKey;
import com.banking.system.transaction.domain.model.value_object.ReferenceNumber;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Transaction {
    private final UUID id;
    private final UUID accountId;
    private final TransactionType transactionType;
    private final Money amount;
    private final Money balanceAfter;
    private final Description description;
    private final ReferenceNumber referenceNumber;
    private final IdempotencyKey idempotencyKey;
    private final Instant executedAt;
    private TransactionStatus status;

    private Transaction(UUID id,
                        UUID accountId,
                        TransactionType transactionType,
                        Money amount,
                        Money balanceAfter,
                        Description description,
                        ReferenceNumber referenceNumber,
                        IdempotencyKey idempotencyKey,
                        TransactionStatus status,
                        Instant executedAt) {

        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(referenceNumber, "Reference number cannot be null");
        Objects.requireNonNull(status, "Transaction status cannot be null");
        Objects.requireNonNull(executedAt, "Executed at timestamp cannot be null");

        this.id = id;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceNumber = referenceNumber;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.executedAt = executedAt;
    }

    /**
     * Factory method for creating a new transaction from the application layer.
     * <p>
     * Creates the transaction in {@link TransactionStatus#PENDING} status.
     * The {@code id} is set to {@code null} so that the persistence layer can generate it.
     * </p>
     *
     * @param accountId       identifier of the account this transaction belongs to (must not be {@code null})
     * @param transactionType type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, FEE) (must not be {@code null})
     * @param amount          {@link Money} value object representing the transaction amount (must not be {@code null})
     * @param balanceAfter    {@link Money} value object representing the account balance after this transaction
     * @param description     {@link Description} value object containing transaction details
     * @param referenceNumber {@link ReferenceNumber} value object for tracking and reconciliation (must not be {@code null})
     * @param idempotencyKey  {@link IdempotencyKey} to prevent duplicate processing (may be {@code null})
     * @return a new {@link Transaction} instance in PENDING status
     */
    public static Transaction createNew(
            UUID accountId,
            TransactionType transactionType,
            Money amount,
            Money balanceAfter,
            Description description,
            ReferenceNumber referenceNumber,
            IdempotencyKey idempotencyKey
    ) {
        return new Transaction(
                null,
                accountId,
                transactionType,
                amount,
                balanceAfter,
                description,
                referenceNumber,
                idempotencyKey,
                TransactionStatus.PENDING,
                Instant.now()
        );
    }

    /**
     * Factory method for reconstituting an existing transaction from persistence.
     */
    public static Transaction reconstitute(
            UUID id,
            UUID accountId,
            TransactionType transactionType,
            Money amount,
            Money balanceAfter,
            Description description,
            ReferenceNumber referenceNumber,
            IdempotencyKey idempotencyKey,
            TransactionStatus status,
            Instant executedAt
    ) {
        return new Transaction(
                id,
                accountId,
                transactionType,
                amount,
                balanceAfter,
                description,
                referenceNumber,
                idempotencyKey,
                status,
                executedAt
        );
    }

    public void markCompleted() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can be marked as COMPLETED");
        }
        this.status = TransactionStatus.COMPLETED;
    }

    public void markFailed() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can be marked as FAILED");
        }
        this.status = TransactionStatus.FAILED;
    }

    public void reverse() {
        if (this.status != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED transactions can be reversed");
        }
        this.status = TransactionStatus.REVERSED;
    }
}
