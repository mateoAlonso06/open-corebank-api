package com.banking.system.account.infraestructure.adapter.out.mapper;

import com.banking.system.account.domain.model.Account;
import com.banking.system.account.domain.model.value_object.AccountAlias;
import com.banking.system.account.domain.model.value_object.AccountNumber;
import com.banking.system.common.domain.Money;
import com.banking.system.common.domain.MoneyCurrency;
import com.banking.system.account.infraestructure.adapter.out.persistence.entity.AccountJpaEntity;

public class AccountJpaMapper {
    public static AccountJpaEntity toJpaEntity(Account account) {
        // Build the JPA entity using the generated builder methods from Lombok
        return AccountJpaEntity.builder()
                .id(account.getId())
                .customerId(account.getCustomerId())
                .accountNumber(account.getAccountNumber().value())
                .alias(account.getAlias().value())
                .accountType(account.getAccountType())
                .currency(account.getCurrency().code())
                .status(account.getStatus())
                .balance(account.getBalance().getValue())
                .availableBalance(account.getAvailableBalance().getValue())
                .dailyTransferLimit(account.getDailyTransferLimit().getValue())
                .monthlyTransferLimit(account.getMonthlyTransferLimit().getValue())
                .openedAt(account.getOpenedAt())
                .version(account.getVersion())
                .build();
    }

    public static Account toDomainEntity(AccountJpaEntity accountJpaEntity) {
        // Rebuild value objects from primitive JPA fields when mapping back to the domain model
        MoneyCurrency currency = MoneyCurrency.ofCode(accountJpaEntity.getCurrency());

        return Account.reconstitute(
                accountJpaEntity.getId(),
                accountJpaEntity.getCustomerId(),
                new AccountNumber(accountJpaEntity.getAccountNumber()),
                new AccountAlias(accountJpaEntity.getAlias()),
                accountJpaEntity.getAccountType(),
                currency,
                accountJpaEntity.getStatus(),
                Money.of(accountJpaEntity.getBalance(), currency),
                Money.of(accountJpaEntity.getAvailableBalance(), currency),
                Money.of(accountJpaEntity.getDailyTransferLimit(), currency),
                Money.of(accountJpaEntity.getMonthlyTransferLimit(), currency),
                accountJpaEntity.getOpenedAt(),
                accountJpaEntity.getClosedAt(),
                accountJpaEntity.getUpdatedAt(),
                accountJpaEntity.getVersion()
        );
    }
}
