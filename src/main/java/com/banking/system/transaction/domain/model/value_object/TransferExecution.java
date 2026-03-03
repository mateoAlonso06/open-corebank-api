package com.banking.system.transaction.domain.model.value_object;

import com.banking.system.transaction.domain.model.Transaction;
import com.banking.system.transaction.domain.model.Transfer;

/**
 * Represents the result of executing a transfer in the domain layer.
 * <p>
 * This record encapsulates all the entities created during a transfer execution,
 * including the transactions for both accounts and the transfer aggregate itself.
 * </p>
 *
 * @param debitTransaction  the TRANSFER_OUT transaction on the source account
 * @param creditTransaction the TRANSFER_IN transaction on the destination account
 * @param feeTransaction    the FEE transaction on the source account (may be null if no fee)
 * @param transfer          the Transfer aggregate linking both transactions
 */
public record TransferExecution(
        Transaction debitTransaction,
        Transaction creditTransaction,
        Transaction feeTransaction,
        Transfer transfer
) {
    public boolean hasFee() {
        return feeTransaction != null;
    }
}