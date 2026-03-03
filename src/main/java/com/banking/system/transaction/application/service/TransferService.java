package com.banking.system.transaction.application.service;

import com.banking.system.account.domain.exception.AccountNotFoundException;
import com.banking.system.account.domain.model.Account;
import com.banking.system.account.domain.port.out.AccountRepositoryPort;
import com.banking.system.common.domain.Money;
import com.banking.system.common.domain.MoneyCurrency;
import com.banking.system.common.domain.exception.DomainException;
import com.banking.system.customer.domain.exception.CustomerNotFoundException;
import com.banking.system.customer.domain.port.out.CustomerRepositoryPort;
import com.banking.system.transaction.application.dto.command.TransferMoneyCommand;
import com.banking.system.transaction.application.dto.receipt.TransferReceipt;
import com.banking.system.transaction.application.dto.result.TransferResult;
import com.banking.system.transaction.application.mapper.ReceiptMapper;
import com.banking.system.transaction.application.mapper.TransferDomainMapper;
import com.banking.system.transaction.application.usecase.GetTransferByIdUseCase;
import com.banking.system.transaction.application.usecase.TransferMoneyUseCase;
import com.banking.system.transaction.domain.exception.KycNotApprovedException;
import com.banking.system.transaction.domain.exception.denied.TransferAccessDeniedException;
import com.banking.system.transaction.domain.exception.notfound.TransactionNotFoundException;
import com.banking.system.transaction.domain.exception.notfound.TransferNotFoundException;
import com.banking.system.transaction.domain.model.value_object.Description;
import com.banking.system.transaction.domain.model.value_object.IdempotencyKey;
import com.banking.system.transaction.domain.model.Transfer;
import com.banking.system.transaction.domain.model.value_object.TransferExecution;
import com.banking.system.transaction.domain.port.out.TransactionRepositoryPort;
import com.banking.system.transaction.domain.port.out.TransferRepositoryPort;
import com.banking.system.transaction.domain.service.TransferDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService implements TransferMoneyUseCase, GetTransferByIdUseCase {

    private final TransferRepositoryPort transferRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final TransferDomainService transferDomainService;
    private final TransactionAuditService transactionAuditService;

    @Override
    @Transactional
    public TransferReceipt transfer(TransferMoneyCommand command, UUID userId) {
        log.info("Initiating transfer from account {}", command.fromAccountId());

        Account sourceAccount = accountRepositoryPort.findById(command.fromAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + command.fromAccountId()));

        validateOwnership(sourceAccount, userId);

        Account targetAccount = resolveTargetAccount(command); // search by alias or account number

        IdempotencyKey idempotencyKey = IdempotencyKey.from(command.idempotencyKey());

        Optional<Transfer> existingTransfer = transferRepositoryPort.findByIdempotencyKey(idempotencyKey.value());
        if (existingTransfer.isPresent()) {
            log.info("Transfer already exists for idempotency key {}", idempotencyKey.value());
            // Return receipt for existing transfer
            var debitTx = transactionRepositoryPort.findById(existingTransfer.get().getDebitTransactionId())
                    .orElseThrow(() -> new TransactionNotFoundException("Debit transaction not found"));
            return ReceiptMapper.toTransferReceipt(existingTransfer.get(), sourceAccount, targetAccount, debitTx);
        }

        TransferExecution execution = transferDomainService.execute(
                sourceAccount,
                targetAccount,
                command.category(),
                toMoney(command.amount(), command.currency()),
                command.description() != null ? new Description(command.description()) : null,
                toFeeAmount(command),
                idempotencyKey
        );

        Transfer transferSaved = persistExecution(execution, sourceAccount, targetAccount);

        log.info("Transfer completed successfully for idempotency key {}", idempotencyKey.value());

        // Return receipt for confirmation/voucher
        return ReceiptMapper.toTransferReceipt(
                transferSaved,
                sourceAccount,
                targetAccount,
                execution.debitTransaction()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResult findByIdForCustomer(UUID transferId, UUID userId) {
        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for userId: " + userId));

        var transfer = transferRepositoryPort.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found: " + transferId));

        var sourceAccount = accountRepositoryPort.findById(transfer.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found"));
        var destinationAccount = accountRepositoryPort.findById(transfer.getDestinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));

        boolean isOwnerOfSource = sourceAccount.getCustomerId().equals(customer.getId());
        boolean isOwnerOfDestination = destinationAccount.getCustomerId().equals(customer.getId());

        if (!isOwnerOfSource && !isOwnerOfDestination) {
            log.warn("Unauthorized transfer access attempt by userId: {} to transferId: {}", userId, transferId);
            throw new TransferAccessDeniedException("Transfer does not belong to the authenticated user");
        }

        return TransferDomainMapper.toResult(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResult findById(UUID transferId) {
        var transfer = transferRepositoryPort.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found: " + transferId));

        return TransferDomainMapper.toResult(transfer);
    }

    private Account resolveTargetAccount(TransferMoneyCommand command) {
        if (command.toAlias() != null) {
            return accountRepositoryPort.findByAlias(command.toAlias())
                    .orElseThrow(() -> new AccountNotFoundException("Target account not found for alias: " + command.toAlias()));
        }// if alias is null then search by account number
        return accountRepositoryPort.findByAccountNumber(command.toAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Target account not found for account number: " + command.toAccountNumber()));
    }

    private Money toMoney(BigDecimal amount, String currency) {
        return Money.of(amount, MoneyCurrency.ofCode(currency));
    }

    private Money toFeeAmount(TransferMoneyCommand command) {
        if (command.feeAmount() == null || command.feeCurrency() == null) {
            return null;
        }
        if (command.feeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return Money.of(command.feeAmount(), MoneyCurrency.ofCode(command.feeCurrency()));
    }

    private Transfer persistExecution(TransferExecution execution, Account sourceAccount, Account targetAccount) {
        // Register transactions in PENDING status
        var savedDebit = transactionAuditService.registerTransactionAudit(execution.debitTransaction());
        var savedCredit = transactionAuditService.registerTransactionAudit(execution.creditTransaction());

        var savedFee = execution.hasFee()
                ? transactionAuditService.registerTransactionAudit(execution.feeTransaction())
                : null;

        try {
            // Save accounts (already modified by domain service)
            accountRepositoryPort.save(sourceAccount);
            accountRepositoryPort.save(targetAccount);

            // Save transfer
            var transferWithIds = Transfer.createNew(
                    execution.transfer().getSourceAccountId(),
                    execution.transfer().getDestinationAccountId(),
                    savedDebit.getId(),
                    savedCredit.getId(),
                    execution.transfer().getCategory(),
                    execution.transfer().getAmount(),
                    execution.transfer().getDescription(),
                    execution.transfer().getFeeAmount(),
                    savedFee != null ? savedFee.getId() : null,
                    execution.transfer().getIdempotencyKey()
            );
            var transferSaved = transferRepositoryPort.save(transferWithIds);

            // Mark transactions as COMPLETED
            transactionAuditService.transactionCompleted(savedDebit.getId());
            transactionAuditService.transactionCompleted(savedCredit.getId());
            if (savedFee != null) {
                transactionAuditService.transactionCompleted(savedFee.getId());
            }
            return transferSaved;
        } catch (DomainException e) {
            // Mark transactions as FAILED
            transactionAuditService.transactionFailed(savedDebit.getId());
            transactionAuditService.transactionFailed(savedCredit.getId());
            if (savedFee != null) {
                transactionAuditService.transactionFailed(savedFee.getId());
            }
            log.error("Transfer execution failed, marking transactions as FAILED", e);
            throw e;
        }
    }

    private void validateOwnership(Account sourceAccount, UUID userId) {
        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for userId: " + userId));

        if (!customer.isKycApproved()) {
            throw new KycNotApprovedException("Customer KYC not approved");
        }

        if (!sourceAccount.getCustomerId().equals(customer.getId())) {
            throw new TransferAccessDeniedException("Source account does not belong to the authenticated user");
        }
    }
}