package com.banking.system.transaction.application.service;

import com.banking.system.account.application.dto.result.AccountLimitResult;
import com.banking.system.account.domain.exception.AccountNotFoundException;
import com.banking.system.account.domain.model.Account;
import com.banking.system.account.domain.model.AccountLimits;
import com.banking.system.account.domain.port.out.AccountRepositoryPort;
import com.banking.system.common.domain.Money;
import com.banking.system.common.domain.MoneyCurrency;
import com.banking.system.common.domain.PageRequest;
import com.banking.system.common.domain.dto.PagedResult;
import com.banking.system.customer.domain.exception.CustomerNotFoundException;
import com.banking.system.customer.domain.model.Customer;
import com.banking.system.customer.domain.port.out.CustomerRepositoryPort;
import com.banking.system.transaction.application.dto.command.DepositMoneyCommand;
import com.banking.system.transaction.application.dto.command.WithdrawMoneyCommand;
import com.banking.system.transaction.application.dto.receipt.TransactionReceipt;
import com.banking.system.transaction.application.dto.result.TransactionResult;
import com.banking.system.transaction.application.mapper.ReceiptMapper;
import com.banking.system.transaction.application.mapper.TransactionDomainMapper;
import com.banking.system.transaction.application.usecase.*;
import com.banking.system.transaction.domain.exception.DailyLimitExceededException;
import com.banking.system.transaction.domain.exception.InvalidTransactionException;
import com.banking.system.transaction.domain.exception.KycNotApprovedException;
import com.banking.system.transaction.domain.exception.MonthlyLimitExceededException;
import com.banking.system.transaction.domain.exception.alreadyexist.TransactionAlreadyExistException;
import com.banking.system.transaction.domain.exception.denied.AccountAccessDeniedException;
import com.banking.system.transaction.domain.exception.notfound.TransactionNotFoundException;
import com.banking.system.transaction.domain.model.*;
import com.banking.system.transaction.domain.port.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements
        DepositUseCase,
        WithdrawUseCase,
        GetTransactionByIdUseCase,
        GetAllTransactionsByAccountUseCase,
        GetAllTransactionsByCustomerUseCase,
        GetTransactionByReferenceNumber,
        GetAccountLimitsUseCase {
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionAuditService transactionAuditService;

    @Override
    @Transactional
    public TransactionReceipt deposit(DepositMoneyCommand command, UUID accountId, UUID userId) {
        log.info("Starting deposit process for userId: {}", userId);

        Money depositAmount = Money.of(command.amount(), MoneyCurrency.ofCode(command.currency()));

        isInvalidTransaction(command.amount(), command.currency(), "deposit");

        IdempotencyKey idempotencyKey = IdempotencyKey.from(command.idempotencyKey());
        checkIdempotency(idempotencyKey);

        Account account = getAuthorizedAccount(accountId, userId);

        validateTransactionLimits(account, depositAmount.getValue(), TransactionType.DEPOSIT);

        // Calculate balance after the operation BEFORE modifying the account
        Money balanceAfter = account.getBalance().add(depositAmount);

        Transaction transaction = Transaction.createNew(
                account.getId(),
                TransactionType.DEPOSIT,
                depositAmount,
                balanceAfter,
                new Description("Deposit of " + depositAmount + " to account " + account.getAccountNumber().value()),
                ReferenceNumber.generate(),
                idempotencyKey
        );

        // Register transaction in PENDING status
        Transaction savedTransaction = transactionAuditService.registerTransactionAudit(transaction);

        try {
            // Execute the account operation
            account.credit(depositAmount);
            accountRepositoryPort.save(account);

            // Mark transaction as COMPLETED
            transactionAuditService.transactionCompleted(savedTransaction.getId());
            log.info("Deposit of {} to accountId: {} completed successfully", depositAmount, account.getId());

            // Return receipt for confirmation/voucher
            return ReceiptMapper.toTransactionReceipt(savedTransaction, account);
        } catch (Exception e) {
            log.debug("ocurrio un error");
            transactionAuditService.transactionFailed(savedTransaction.getId());
            log.error("Transaction failed for operation deposit, transactionId: {}", savedTransaction.getId(), e);
            throw e;
        }
    }

    private void isInvalidTransaction(BigDecimal amount, String currency, String label) {
        Money transactionAmount = Money.of(amount, MoneyCurrency.ofCode(currency));
        if (transactionAmount.isZero() || transactionAmount.isNegative()) {
            throw new InvalidTransactionException(label + " amount must be greater than zero");
        }
    }

    @Override
    @Transactional
    public TransactionReceipt withdraw(WithdrawMoneyCommand command, UUID accountId, UUID userId) {
        log.info("Starting withdrawal process for userId: {}", userId);

        Money withdrawAmount = Money.of(command.amount(), MoneyCurrency.ofCode(command.currency()));

        isInvalidTransaction(command.amount(), command.currency(), "withdrawal");

        IdempotencyKey idempotencyKey = IdempotencyKey.from(command.idempotencyKey());
        checkIdempotency(idempotencyKey);

        Account account = getAuthorizedAccount(accountId, userId);

        validateTransactionLimits(account, withdrawAmount.getValue(), TransactionType.WITHDRAWAL);

        // Calculate balance after the operation BEFORE modifying the account
        Money balanceAfter = account.getBalance().subtract(withdrawAmount);

        Transaction transaction = Transaction.createNew(
                account.getId(),
                TransactionType.WITHDRAWAL,
                withdrawAmount,
                balanceAfter,
                new Description("Withdrawal of " + withdrawAmount + " from account " + account.getAccountNumber().value()),
                ReferenceNumber.generate(),
                idempotencyKey
        );

        // Register transaction in PENDING status
        Transaction savedTransaction = transactionAuditService.registerTransactionAudit(transaction);

        try {
            // Execute the account operation
            account.debit(withdrawAmount);
            accountRepositoryPort.save(account);

            // Mark transaction as COMPLETED
            transactionAuditService.transactionCompleted(savedTransaction.getId());
            log.info("Withdrawal of {} from accountId: {} completed successfully", withdrawAmount, account.getId());

            // Return receipt for confirmation/voucher
            return ReceiptMapper.toTransactionReceipt(savedTransaction, account);
        } catch (Exception e) {
            transactionAuditService.transactionFailed(savedTransaction.getId());
            log.error("Transaction failed for operation withdrawal, transactionId: {}", savedTransaction.getId(), e);
            throw e;
        }
    }

    /*For the history*/
    @Override
    @Transactional(readOnly = true)
    public PagedResult<TransactionResult> getAllTransactionsByAccountId(UUID accountId, UUID userId, PageRequest pageRequest) {
        this.getAuthorizedAccount(accountId, userId);
        PagedResult<Transaction> transactionsPage = transactionRepositoryPort.findAllTransactionsByAccountId(pageRequest, accountId);

        return PagedResult.mapContent(transactionsPage, TransactionDomainMapper::toResult);
    }


    @Override
    @Transactional(readOnly = true)
    public PagedResult<TransactionResult> getAllTransactionsByCustomer(UUID userId, PageRequest pageRequest) {
        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for userId: " + userId));

        if (!customer.isKycApproved()) {
            throw new KycNotApprovedException("KYC not approved for the customer");
        }

        List<Account> accounts = accountRepositoryPort.findAllByCustomerId(customer.getId());

        List<UUID> accountIds = accounts.stream()
                .map(Account::getId)
                .toList();

        PagedResult<Transaction> transactions = transactionRepositoryPort
                .findAllByAccountIdsAndStatus(accountIds, TransactionStatus.COMPLETED, pageRequest);

        return PagedResult.mapContent(transactions, TransactionDomainMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountLimitResult getAccountLimits(UUID accountId, UUID userId) {
        Account account = getAuthorizedAccount(accountId, userId);
        AccountLimits limits = AccountLimits.forType(account.getAccountType());

        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfMonth = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        BigDecimal depositToday = transactionRepositoryPort.sumCompletedAmountByAccountIdAndTypeSince(account.getId(), TransactionType.DEPOSIT, startOfDay);
        BigDecimal depositMonth = transactionRepositoryPort.sumCompletedAmountByAccountIdAndTypeSince(account.getId(), TransactionType.DEPOSIT, startOfMonth);
        BigDecimal withdrawalToday = transactionRepositoryPort.sumCompletedAmountByAccountIdAndTypeSince(account.getId(), TransactionType.WITHDRAWAL, startOfDay);
        BigDecimal withdrawalMonth = transactionRepositoryPort.sumCompletedAmountByAccountIdAndTypeSince(account.getId(), TransactionType.WITHDRAWAL, startOfMonth);

        return AccountLimitResult.of(account, limits, depositToday, depositMonth, withdrawalToday, withdrawalMonth);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResult getTransactionById(UUID transactionId, UUID userId) {
        log.debug("initiating getTransactionById for transactionId: {} and userId: {}", transactionId, userId);

        Transaction transaction = transactionRepositoryPort.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));

        // Checks if the user has access to the account related to the transaction, if not, it will throw an exception
        getAuthorizedAccount(transaction.getAccountId(), userId);

        return TransactionDomainMapper.toResult(transaction);
    }

    @Override
    public TransactionResult getTransactionByReferenceNumber(String referenceNumber, UUID userId) {
        Transaction transaction = transactionRepositoryPort.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with reference number: " + referenceNumber));

        // Checks if the user has access to the account related to the transaction, if not, it will throw an exception
        getAuthorizedAccount(transaction.getAccountId(), userId);

        return TransactionDomainMapper.toResult(transaction);
    }

    /*Gets the account if the user is authorized and KYC is approved*/
    private @NonNull Account getAuthorizedAccount(UUID accountId, UUID userId) {
        Customer customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for userId: " + userId));

        Account account = accountRepositoryPort.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        if (!account.getCustomerId().equals(customer.getId())) {
            log.warn("Unauthorized account access attempt by userId: {} to accountId: {}", userId, accountId);
            throw new AccountAccessDeniedException("Account does not belong to the authenticated user");
        }

        if (!customer.isKycApproved()) {
            log.warn("KYC not approved for userId: {}", userId);
            throw new KycNotApprovedException("KYC not approved for the customer");
        }
        return account;
    }

    private void validateTransactionLimits(Account account, BigDecimal amount, TransactionType type) {
        AccountLimits limits = AccountLimits.forType(account.getAccountType());

        // If is not a deposit is a withdrawal, so we need to check the limits for the type of transaction
        BigDecimal dailyLimit = (type == TransactionType.DEPOSIT)
                ? limits.dailyDepositLimit() : limits.dailyWithdrawalLimit();
        BigDecimal monthlyLimit = (type == TransactionType.DEPOSIT)
                ? limits.monthlyDepositLimit() : limits.monthlyWithdrawalLimit();

        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal todayTotal = transactionRepositoryPort
                .sumCompletedAmountByAccountIdAndTypeSince(account.getId(), type, startOfDay);
        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new DailyLimitExceededException(dailyLimit, todayTotal, amount);
        }

        Instant startOfMonth = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal monthTotal = transactionRepositoryPort
                .sumCompletedAmountByAccountIdAndTypeSince(account.getId(), type, startOfMonth);
        if (monthTotal.add(amount).compareTo(monthlyLimit) > 0) {
            throw new MonthlyLimitExceededException(monthlyLimit, monthTotal, amount);
        }
    }

    private void checkIdempotency(IdempotencyKey idempotencyKey) {
        transactionRepositoryPort.findByIdempotencyKey(idempotencyKey.value())
                .ifPresent(existing -> {
                    throw new TransactionAlreadyExistException(
                            "Transaction with idempotency key already exists: " + idempotencyKey.value()
                    );
                });
    }
}