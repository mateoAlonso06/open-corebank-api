package com.banking.system.transaction.domain.service;

import com.banking.system.account.domain.model.Account;
import com.banking.system.common.domain.Money;
import com.banking.system.transaction.domain.exception.SameAccountTransferException;
import com.banking.system.transaction.domain.model.*;
import com.banking.system.transaction.domain.model.enums.TransactionType;
import com.banking.system.transaction.domain.model.enums.TransferCategory;
import com.banking.system.transaction.domain.model.value_object.Description;
import com.banking.system.transaction.domain.model.value_object.IdempotencyKey;
import com.banking.system.transaction.domain.model.value_object.ReferenceNumber;
import com.banking.system.transaction.domain.model.value_object.TransferExecution;
import org.springframework.stereotype.Component;

@Component
public class TransferDomainService {

    /**
     * Executes a complete transfer between two accounts.
     * <p>
     * This method handles all domain logic for a transfer:
     * <ul>
     *   <li>Validates that source and target accounts are different</li>
     *   <li>Validates currency compatibility</li>
     *   <li>Creates debit (TRANSFER_OUT) and credit (TRANSFER_IN) transactions</li>
     *   <li>Creates fee transaction if applicable</li>
     *   <li>Debits source account and credits target account</li>
     *   <li>Creates the Transfer aggregate</li>
     * </ul>
     * </p>
     *
     * @param sourceAccount  the account from which money will be debited
     * @param targetAccount  the account to which money will be credited
     * @param amount         the amount to transfer
     * @param description    description of the transfer
     * @param feeAmount      optional fee amount (may be null)
     * @param idempotencyKey unique key to prevent duplicate transfers
     * @return TransferExecution containing all created entities
     */
    public TransferExecution execute(
            Account sourceAccount,
            Account targetAccount,
            TransferCategory category,
            Money amount,
            Description description,
            Money feeAmount,
            IdempotencyKey idempotencyKey
    ) {
        validateDifferentAccounts(sourceAccount, targetAccount);

        // Debit source account and create transaction with correct balanceAfter
        sourceAccount.debit(amount);
        Transaction debitTx = createDebitTransaction(sourceAccount, amount, description);

        // Debit fee from source account if applicable
        Transaction feeTx = null;
        if (feeAmount != null && !feeAmount.isZero()) {
            sourceAccount.debit(feeAmount);
            feeTx = createFeeTransaction(sourceAccount, feeAmount);
        }

        // Credit target account and create transaction with correct balanceAfter
        targetAccount.credit(amount);
        Transaction creditTx = createCreditTransaction(targetAccount, amount, description);

        Transfer transfer = createTransfer(
                sourceAccount,
                targetAccount,
                category,
                amount,
                description,
                feeAmount,
                idempotencyKey
        );

        return new TransferExecution(debitTx, creditTx, feeTx, transfer);
    }

    private void validateDifferentAccounts(Account sourceAccount, Account targetAccount) {
        if (sourceAccount.getId().equals(targetAccount.getId())) {
            throw new SameAccountTransferException("Source and target accounts must be different");
        }
    }

    private Transaction createDebitTransaction(Account account, Money amount, Description description) {
        return Transaction.createNew(
                account.getId(),
                TransactionType.TRANSFER_OUT,
                amount,
                account.getBalance(),
                description,
                ReferenceNumber.generate(),
                null
        );
    }

    private Transaction createCreditTransaction(Account account, Money amount, Description description) {
        return Transaction.createNew(
                account.getId(),
                TransactionType.TRANSFER_IN,
                amount,
                account.getBalance(),
                description,
                ReferenceNumber.generate(),
                null
        );
    }

    private Transaction createFeeTransaction(Account account, Money feeAmount) {
        if (feeAmount == null || feeAmount.isZero()) {
            return null;
        }
        return Transaction.createNew(
                account.getId(),
                TransactionType.FEE,
                feeAmount,
                account.getBalance(),
                new Description("Transfer Fee"),
                ReferenceNumber.generate(),
                null
        );
    }

    private Transfer createTransfer(
            Account sourceAccount,
            Account targetAccount,
            TransferCategory category,
            Money amount,
            Description description,
            Money feeAmount,
            IdempotencyKey idempotencyKey
    ) {
        return Transfer.createNew(
                sourceAccount.getId(),
                targetAccount.getId(),
                null,
                null,
                category,
                amount,
                description,
                feeAmount,
                null,
                idempotencyKey
        );
    }
}