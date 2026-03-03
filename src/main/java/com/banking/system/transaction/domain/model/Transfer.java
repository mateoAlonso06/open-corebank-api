package com.banking.system.transaction.domain.model;

import com.banking.system.common.domain.Money;
import com.banking.system.transaction.domain.model.enums.TransferCategory;
import com.banking.system.transaction.domain.model.value_object.Description;
import com.banking.system.transaction.domain.model.value_object.IdempotencyKey;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Transfer {
    private final UUID id;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final UUID debitTransactionId;
    private final UUID creditTransactionId;
    private final TransferCategory category;
    private final Money amount;
    private final Money feeAmount;
    private final Description description;
    private final UUID feeTransactionId;
    private final IdempotencyKey idempotencyKey;
    private final Instant executedAt;

    private Transfer(UUID id,
                     UUID sourceAccountId,
                     UUID destinationAccountId,
                     UUID debitTransactionId,
                     UUID creditTransactionId,
                     TransferCategory category,
                     Money amount,
                     Money feeAmount,
                     Description description,
                     UUID feeTransactionId,
                     IdempotencyKey idempotencyKey,
                     Instant executedAt) {

        Objects.requireNonNull(destinationAccountId, "Destination account ID cannot be null");
        Objects.requireNonNull(amount, "Money cannot be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(executedAt, "Executed at timestamp cannot be null");

        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.debitTransactionId = debitTransactionId;
        this.creditTransactionId = creditTransactionId;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.category = category;
        this.description = description;
        this.feeTransactionId = feeTransactionId;
        this.idempotencyKey = idempotencyKey;
        this.executedAt = executedAt;
    }

    /**
     * Factory method for creating a new transfer aggregate from the application layer.
     * <p>
     * This method should be used when recording a brand new transfer that does not
     * yet exist in the persistence layer. It intentionally sets the {@code id} to
     * {@code null} so that the persistence mechanism can generate it, and sets
     * {@code executedAt} to {@link Instant#now()}.
     * </p>
     * <p>
     * A transfer represents a coordinated money movement between two accounts, linking
     * a debit transaction (TRANSFER_OUT) and a credit transaction (TRANSFER_IN),
     * and optionally a fee transaction.
     * </p>
     *
     * @param sourceAccountId      identifier of the account from which money is transferred (must not be {@code null})
     * @param destinationAccountId identifier of the account to which money is transferred (must not be {@code null})
     * @param amount               {@link Money} value object representing the transfer amount (must not be {@code null})
     * @param description          {@link Description} value object containing transfer details (may be {@code null})
     * @param feeAmount            {@link Money} value object representing the fee charged for the transfer (may be {@code null})
     * @param idempotencyKey       {@link IdempotencyKey} value object ensuring transfer uniqueness and preventing duplicates (must not be {@code null})
     * @return a new {@link Transfer} instance representing a non-persisted transfer
     */
    public static Transfer createNew(
            UUID sourceAccountId,
            UUID destinationAccountId,
            UUID debitTransactionId,
            UUID creditTransactionId,
            TransferCategory category,
            Money amount,
            Description description,
            Money feeAmount,
            UUID feeTransactionId,
            IdempotencyKey idempotencyKey
    ) {
        return new Transfer(
                null,
                sourceAccountId,
                destinationAccountId,
                debitTransactionId,
                creditTransactionId,
                category != null ? category : TransferCategory.OTHERS, // default category
                amount,
                feeAmount,
                description,
                feeTransactionId,
                idempotencyKey,
                Instant.now()
        );
    }

    /**
     * Factory method for reconstituting an existing transfer aggregate from persistence.
     * <p>
     * This should be used by repository implementations when loading a transfer
     * from the database. All fields, including {@code id} and {@code executedAt},
     * are expected to come from a trusted source (e.g. JPA entity, database row)
     * and represent the actual state of the transfer.
     * </p>
     *
     * @param id                   unique identifier that has already been generated by the persistence layer
     * @param sourceAccountId      identifier of the account from which money was transferred
     * @param destinationAccountId identifier of the account to which money was transferred
     * @param debitTransactionId   identifier of the TRANSFER_OUT transaction on the source account
     * @param creditTransactionId  identifier of the TRANSFER_IN transaction on the destination account
     * @param amount               {@link Money} value object reconstituted from the persisted amount
     * @param feeAmount            {@link Money} value object reconstituted from the persisted fee (may be {@code null})
     * @param description          {@link Description} value object reconstituted from the persisted description (may be {@code null})
     * @param feeTransactionId     identifier of the fee transaction (may be {@code null})
     * @param idempotencyKey       {@link IdempotencyKey} value object reconstituted from the persisted key
     * @param executedAt           timestamp when the transfer was executed
     * @return a fully initialized {@link Transfer} instance representing an existing transfer
     */
    public static Transfer reconstitute(
            UUID id,
            UUID sourceAccountId,
            UUID destinationAccountId,
            UUID debitTransactionId,
            UUID creditTransactionId,
            Money amount,
            TransferCategory category,
            Money feeAmount,
            Description description,
            UUID feeTransactionId,
            IdempotencyKey idempotencyKey,
            Instant executedAt
    ) {
        return new Transfer(
                id,
                sourceAccountId,
                destinationAccountId,
                debitTransactionId,
                creditTransactionId,
                category,
                amount,
                feeAmount,
                description,
                feeTransactionId,
                idempotencyKey,
                executedAt
        );
    }
}
