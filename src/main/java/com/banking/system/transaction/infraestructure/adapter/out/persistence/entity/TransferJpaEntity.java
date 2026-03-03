package com.banking.system.transaction.infraestructure.adapter.out.persistence.entity;

import com.banking.system.transaction.domain.model.enums.TransferCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transfers")
public class TransferJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debit_transaction_id", nullable = false)
    private TransactionJpaEntity debitTransaction;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_transaction_id", nullable = false)
    private TransactionJpaEntity creditTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_transaction_id")
    private TransactionJpaEntity feeTransaction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransferCategory category;

    @Column(name = "fee_amount", precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column
    private String description;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TransferJpaEntity that = (TransferJpaEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
