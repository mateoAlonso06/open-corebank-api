package com.banking.system.account.application.service;

import com.banking.system.account.application.dto.command.CreateAccountCommand;
import com.banking.system.account.application.dto.result.AccountBalanceResult;
import com.banking.system.account.application.dto.result.AccountPublicResult;
import com.banking.system.account.application.dto.result.AccountResult;
import com.banking.system.account.application.event.AccountCreatedEvent;
import com.banking.system.account.application.event.publisher.AccountEventPublisher;
import com.banking.system.account.application.usecase.*;
import com.banking.system.account.domain.exception.AccountAlreadyExistsException;
import com.banking.system.account.domain.exception.AccountHasNonZeroBalanceException;
import com.banking.system.account.domain.exception.AccountNotFoundException;
import com.banking.system.account.domain.exception.AliasGenerationFailedException;
import com.banking.system.account.domain.exception.InvalidAccountOwnerException;
import com.banking.system.account.domain.model.Account;
import com.banking.system.account.domain.model.enums.AccountType;
import com.banking.system.account.domain.port.out.AccountAliasGenerator;
import com.banking.system.account.domain.port.out.AccountNumberGenerator;
import com.banking.system.account.domain.port.out.AccountRepositoryPort;
import com.banking.system.common.domain.MoneyCurrency;
import com.banking.system.customer.domain.port.out.CustomerRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService implements
        CreateAccountUseCase,
        FindAccountByIdUseCase,
        FindAllAccountsByUserId,
        GetAccountBalanceUseCase,
        SearchAccountByAliasUseCase,
        CloseAllAccountsUseCase {

    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 5;

    private final AccountRepositoryPort accountRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AccountAliasGenerator accountAliasGenerator;
    private final AccountEventPublisher accountEventPublisher;

    @Override
    @Transactional
    public AccountResult createAccount(CreateAccountCommand command, UUID userId) {
        log.info("Creating account: userId={}, accountType={}, currency={}", userId, command.accountType(), command.currency());

        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new InvalidAccountOwnerException("Customer not found for user ID " + userId));

        if (!customer.isKycApproved()) {
            log.debug("KYC not approved for customer: {}", customer.getId());
            throw new IllegalStateException("Customer with ID " + customer.getId() + " has not completed KYC.");
        }

        if (accountRepositoryPort.existsByCustomerIdAndTypeAndCurrency(customer.getId(), command.accountType(), command.currency())) {
            log.debug("Account already exists for customer={}, type={}, currency={}", customer.getId(), command.accountType(), command.currency());
            throw new AccountAlreadyExistsException(
                    "Customer already has a " + command.accountType() + " account in " + command.currency()
            );
        }

        // The alias has more potential for collisions, so we generate and check it in a loop
        var account = this.createAccountWithUniqueAlias(
                customer.getId(),
                command.accountType(),
                MoneyCurrency.ofCode(command.currency())
        );

        Account savedAccount = accountRepositoryPort.save(account);

        log.info("Account created: id={}, number={}, customer={}",
                savedAccount.getId(),
                savedAccount.getAccountNumber(),
                customer.getId());

        accountEventPublisher.publishAccountCreated(
                new AccountCreatedEvent(
                        savedAccount.getId(),
                        savedAccount.getCustomerId(),
                        customer.getUserId(),
                        savedAccount.getCurrency().code(),
                        savedAccount.getBalance().getValue(),
                        savedAccount.getAccountNumber().value(),
                        savedAccount.getAlias().value(),
                        savedAccount.getAccountType().name(),
                        savedAccount.getOpenedAt()
                )
        );

        return AccountResult.fromDomain(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResult findAccountByIdForCustomer(UUID accountId, UUID userId) {
        var customerAssociatedWithAccount = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new InvalidAccountOwnerException("Customer not found for account ID " + accountId));

        var account = accountRepositoryPort.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found."));

        if (!account.getCustomerId().equals(customerAssociatedWithAccount.getId())) {
            throw new InvalidAccountOwnerException("Account with ID " + accountId + " does not belong to the customer.");
        }

        return AccountResult.fromDomain(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResult findAccountById(UUID accountId) {
        var account = accountRepositoryPort.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found."));

        return AccountResult.fromDomain(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResult> findAll(UUID userId) {
        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new InvalidAccountOwnerException("Customer not found for user ID " + userId));

        var accounts = accountRepositoryPort.findAllByCustomerId(customer.getId());

        return accounts.stream()
                .map(AccountResult::fromDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResult getBalance(UUID accountId, UUID userId) {
        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new InvalidAccountOwnerException("Customer not found for user ID " + userId));

        var account = accountRepositoryPort.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found."));

        if (!account.getCustomerId().equals(customer.getId())) {
            throw new InvalidAccountOwnerException("Account with ID " + accountId + " does not belong to the customer.");
        }

        return AccountBalanceResult.fromDomain(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountPublicResult searchByAlias(String alias) {
        var account = accountRepositoryPort.findByAlias(alias)
                .orElseThrow(() -> new AccountNotFoundException("Account with alias '" + alias + "' not found."));

        var customer = customerRepositoryPort.findById(account.getCustomerId())
                .orElseThrow(() -> new AccountNotFoundException("Owner not found for account with alias '" + alias + "'."));

        return new AccountPublicResult(
                account.getAlias().value(),
                customer.getPersonName().fullName(),
                customer.getIdentityDocument().number(),
                account.getCurrency().code(),
                account.getAccountType().name()
        );
    }

    @Transactional
    public void closeAllAccountsForCustomer(UUID userId) {
        var customer = customerRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new InvalidAccountOwnerException("Customer not found for user ID " + userId));

        var accounts = accountRepositoryPort.findAllByCustomerId(customer.getId());

        for (Account account : accounts) {
            if (account.isActive()) {
                if (!account.getBalance().isZero()) {
                    log.warn("Attempting to close account with non-zero balance: accountId={}, balance={}", account.getId(), account.getBalance().getValue());
                    throw new AccountHasNonZeroBalanceException(
                            "Cannot close account with non-zero balance. Account ID: " + account.getId());
                }
                account.close();
                accountRepositoryPort.save(account);
                log.info("Closed account: id={}, number={}, customer={}",
                        account.getId(),
                        account.getAccountNumber(),
                        customer.getId());
            }
        }
    }

    /**
     * Creates account with unique alias, retrying if collision occurs.
     * This handles the rare case where the randomly generated alias already exists.
     */
    private Account createAccountWithUniqueAlias(UUID customerId, AccountType accountType, MoneyCurrency currency) {
        var accountNumber = accountNumberGenerator.generate(accountType);

        for (int attempt = 0; attempt < MAX_ALIAS_GENERATION_ATTEMPTS; attempt++) {
            var candidateAlias = accountAliasGenerator.generate();

            if (!accountRepositoryPort.existsByAlias(candidateAlias.value())) {
                log.debug("Alias generated on attempt {}: {}", attempt + 1, candidateAlias.value());
                return Account.createNewAccount(
                        customerId, accountType, currency, accountNumber, candidateAlias
                );
            }
            log.debug("Alias collision on attempt {}: {}", attempt + 1, candidateAlias.value());
        }
        throw new AliasGenerationFailedException("Failed to generate unique alias after " + MAX_ALIAS_GENERATION_ATTEMPTS + " attempts");
    }
}
