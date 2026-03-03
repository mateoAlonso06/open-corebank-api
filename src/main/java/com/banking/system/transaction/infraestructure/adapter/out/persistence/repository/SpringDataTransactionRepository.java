package com.banking.system.transaction.infraestructure.adapter.out.persistence.repository;

import com.banking.system.transaction.domain.model.enums.TransactionStatus;
import com.banking.system.transaction.domain.model.enums.TransactionType;
import com.banking.system.transaction.infraestructure.adapter.out.persistence.entity.TransactionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {
    Page<TransactionJpaEntity> findAllByAccountId(UUID accountId, Pageable pageable);

    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Page<TransactionJpaEntity> findAllByAccountIdIn(List<UUID> accountIds, Pageable pageable);

    Page<TransactionJpaEntity> findAllByAccountIdInAndStatus(List<UUID> accountIds, TransactionStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionJpaEntity t " +
            "WHERE t.accountId = :accountId AND t.transactionType = :type " +
            "AND t.status = 'COMPLETED' AND t.executedAt >= :since")
    BigDecimal sumCompletedAmountByAccountIdAndTypeSince(
            @Param("accountId") UUID accountId,
            @Param("type") TransactionType type,
            @Param("since") Instant since);

    Optional<TransactionJpaEntity> findByReferenceNumber(String referenceNumber);
}
