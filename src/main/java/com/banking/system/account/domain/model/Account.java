package com.banking.system.account.domain.model;

import com.banking.system.account.domain.exception.AccountNotActiveException;
import com.banking.system.account.domain.exception.InsufficientFundsException;
import com.banking.system.account.domain.exception.InvalidAmountException;
import com.banking.system.account.domain.model.enums.AccountStatus;
import com.banking.system.account.domain.model.enums.AccountType;
import com.banking.system.account.domain.model.value_object.AccountAlias;
import com.banking.system.account.domain.model.value_object.AccountLimits;
import com.banking.system.account.domain.model.value_object.AccountNumber;
import com.banking.system.common.domain.Money;
import com.banking.system.common.domain.MoneyCurrency;
import com.banking.system.common.domain.exception.CurrencyMismatchException;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Account {
    private final UUID id;
    private final UUID customerId;
    private final AccountNumber accountNumber;
    private final AccountType accountType;
    private final MoneyCurrency currency;
    private final LocalDate openedAt;
    private AccountAlias alias;
    private Money balance;
    private Money availableBalance;
    private Money dailyTransferLimit;
    private Money monthlyTransferLimit;
    private AccountStatus status;
    private LocalDate closedAt;
    private Instant updatedAt;
    private Long version;

    private Account(
            UUID id,
            UUID customerId,
            AccountNumber accountNumber,
            AccountAlias alias,
            AccountType accountType,
            MoneyCurrency currency,
            AccountStatus status,
            Money balance,
            Money availableBalance,
            Money dailyTransferLimit,
            Money monthlyTransferLimit,
            LocalDate openedAt,
            LocalDate closedAt,
            Instant updatedAt,
            Long version) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(accountNumber, "Account number cannot be null");
        Objects.requireNonNull(accountType, "Account type cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(status, "Account status cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(availableBalance, "Available balance cannot be null");
        Objects.requireNonNull(dailyTransferLimit, "Daily transfer limit cannot be null");
        Objects.requireNonNull(monthlyTransferLimit, "Monthly withdrawal limit cannot be null");
        Objects.requireNonNull(openedAt, "Opened at timestamp cannot be null");

        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.alias = alias;
        this.accountType = accountType;
        this.currency = currency;
        this.status = status;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.dailyTransferLimit = dailyTransferLimit;
        this.monthlyTransferLimit = monthlyTransferLimit;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private void validatePositiveAmount(Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw new InvalidAmountException("Amount must be positive: " + amount);
        }
    }

    /**
     * Reconstitutes an existing {@link Account} aggregate from persisted state.
     * <p>
     * This factory is intended for repository implementations when loading an account
     * from the database. All invariants are still enforced by the constructor
     * preconditions.
     *
     * @param id                   unique identifier of the account
     * @param customerId           identifier of the account owner
     * @param accountNumber        business account number
     * @param alias                optional human-friendly alias
     * @param accountType          type of the account (e.g. checking, savings)
     * @param currency             currency of the account balances
     * @param status               current lifecycle status of the account
     * @param balance              current booked balance
     * @param availableBalance     current available balance
     * @param dailyTransferLimit   configured daily transfer limit
     * @param monthlyTransferLimit configured monthly transfer limit
     * @param openedAt             date when the account was opened
     * @param closedAt             date when the account was closed, or {@code null} if active
     * @param updatedAt            timestamp of the last update
     * @return fully initialized {@link Account} instance representing existing data
     */
    public static Account reconstitute(
            UUID id,
            UUID customerId,
            AccountNumber accountNumber,
            AccountAlias alias,
            AccountType accountType,
            MoneyCurrency currency,
            AccountStatus status,
            Money balance,
            Money availableBalance,
            Money dailyTransferLimit,
            Money monthlyTransferLimit,
            LocalDate openedAt,
            LocalDate closedAt,
            Instant updatedAt,
            Long version) {

        return new Account(
                id,
                customerId,
                accountNumber,
                alias,
                accountType,
                currency,
                status,
                balance,
                availableBalance,
                dailyTransferLimit,
                monthlyTransferLimit,
                openedAt,
                closedAt,
                updatedAt,
                version
        );
    }

    /**
     * Creates a new {@link Account} aggregate for an account opening use case.
     * <p>
     * The identifier is left {@code null} so it can be assigned by the persistence
     * layer. The account starts in {@link AccountStatus#ACTIVE} status with zero
     * balances and default limits, and {@code openedAt} is set to {@link LocalDate#now()}.
     *
     * @param customerId    identifier of the account owner
     * @param accountType   requested type of the new account
     * @param currency      currency of the new account
     * @param accountNumber generated business account number
     * @param alias         optional alias for the new account
     * @return new {@link Account} instance ready to be persisted
     */
    public static Account createNewAccount(
            UUID customerId,
            AccountType accountType,
            MoneyCurrency currency,
            AccountNumber accountNumber,
            AccountAlias alias) {

        AccountLimits limits = AccountLimits.forType(accountType);

        return new Account(
                null, // id - assigned by persistence
                customerId,
                accountNumber,
                alias,
                accountType,
                currency,
                AccountStatus.ACTIVE,
                Money.zero(currency),
                Money.zero(currency),
                Money.of(limits.dailyTransferLimit(), currency),
                Money.of(limits.monthlyTransferLimit(), currency),
                LocalDate.now(),
                null,
                Instant.now(),
                0L
        );
    }

    public void debit(Money amount) {
        Objects.requireNonNull(amount, "Debit amount cannot be null");
        validateActiveAccount();
        validateSameCurrency(amount.getCurrency());
        validatePositiveAmount(amount);
        validateSufficientFunds(amount);

        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    public void credit(Money amount) {
        Objects.requireNonNull(amount, "Credit amount cannot be null");
        validateActiveAccount();
        validateSameCurrency(amount.getCurrency());
        validatePositiveAmount(amount);

        this.balance = this.balance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public void close() {
        validateActiveAccount();
        this.status = AccountStatus.CLOSED;
        this.closedAt = LocalDate.now();
    }

    private void validateSameCurrency(@NonNull MoneyCurrency currency) {
        if (!this.currency.value().equals(currency.value())) {
            throw new CurrencyMismatchException("Account currency " + this.currency.value() + " does not match operation currency " + currency.value());
        }
    }

    private void validateActiveAccount() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account with id " + this.id + " is not active");
        }
    }

    private void validateSufficientFunds(Money amount) {
        if (this.availableBalance.subtract(amount).isNegative()) {
            throw new InsufficientFundsException(this.id, amount, this.availableBalance);
        }
    }
}
