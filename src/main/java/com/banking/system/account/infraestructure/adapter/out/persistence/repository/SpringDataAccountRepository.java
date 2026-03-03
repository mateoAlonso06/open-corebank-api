package com.banking.system.account.infraestructure.adapter.out.persistence.repository;

import com.banking.system.account.domain.model.enums.AccountType;
import com.banking.system.account.infraestructure.adapter.out.persistence.entity.AccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {
    boolean existsByCustomerIdAndAccountTypeAndCurrency(UUID customerId, AccountType accountType, String currency);

    Optional<AccountJpaEntity> findByAlias(String alias);

    boolean existsByAlias(String alias);

    boolean existsByAccountNumber(String accountNumber);

    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);

    List<AccountJpaEntity> findAllByCustomerId(UUID customerId);

    Optional<AccountJpaEntity> findByCustomerId(UUID customerId);

    boolean existsByCustomerId(UUID customerId);
}
