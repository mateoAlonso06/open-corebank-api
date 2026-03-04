package com.banking.system.transaction.application.mapper;

import com.banking.system.account.domain.model.Account;
import com.banking.system.transaction.application.dto.receipt.AccountInfo;
import com.banking.system.transaction.application.dto.receipt.TransactionReceipt;
import com.banking.system.transaction.application.dto.receipt.TransferReceipt;
import com.banking.system.transaction.domain.model.Transaction;
import com.banking.system.transaction.domain.model.Transfer;
import com.banking.system.transaction.domain.model.enums.TransactionStatus;

/**
 * Mapper for creating receipts (vouchers/confirmations) from domain models.
 * Receipts contain masked/optimized information for displaying to users.
 */
public class ReceiptMapper {

    /**
     * Creates a transaction receipt for deposit/withdrawal operations.
     */
    public static TransactionReceipt toTransactionReceipt(Transaction transaction, Account account) {
        return new TransactionReceipt(
                transaction.getId(),
                transaction.getReferenceNumber().value(),
                transaction.getTransactionType().name(),
                transaction.getAmount().getValue(),
                transaction.getAmount().getCurrency().code(),
                transaction.getBalanceAfter() != null ? transaction.getBalanceAfter().getValue() : null,
                maskAccountNumber(account.getAccountNumber().value()),
                account.getAlias() != null ? account.getAlias().value() : null,
                transaction.getDescription() != null ? transaction.getDescription().value() : null,
                TransactionStatus.COMPLETED.name(),
                transaction.getExecutedAt()
        );
    }

    /**
     * Creates a transfer receipt with source and destination account info.
     */
    public static TransferReceipt toTransferReceipt(
            Transfer transfer,
            Account sourceAccount,
            Account destinationAccount,
            Transaction debitTransaction
    ) {
        return new TransferReceipt(
                transfer.getId(),
                debitTransaction.getReferenceNumber().value(),
                transfer.getCategory(),
                transfer.getAmount().getValue(),
                transfer.getAmount().getCurrency().code(),
                transfer.getFeeAmount() != null ? transfer.getFeeAmount().getValue() : null,
                transfer.getDescription() != null ? transfer.getDescription().value() : null,
                transfer.getExecutedAt(),
                toAccountInfo(sourceAccount),
                toAccountInfo(destinationAccount)
        );
    }

    /**
     * Creates masked account info for receipts.
     */
    private static AccountInfo toAccountInfo(Account account) {
        return new AccountInfo(
                account.getAlias() != null ? account.getAlias().value() : null,
                maskAccountNumber(account.getAccountNumber().value())
        );
    }

    /**
     * Masks account number showing only last 4 digits.
     * Example: "1234567890123456789012" → "****9012"
     */
    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }
}
