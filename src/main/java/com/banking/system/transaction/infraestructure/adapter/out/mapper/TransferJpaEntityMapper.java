package com.banking.system.transaction.infraestructure.adapter.out.mapper;

import com.banking.system.common.domain.Money;
import com.banking.system.common.domain.MoneyCurrency;
import com.banking.system.transaction.domain.model.value_object.Description;
import com.banking.system.transaction.domain.model.value_object.IdempotencyKey;
import com.banking.system.transaction.domain.model.Transfer;
import com.banking.system.transaction.infraestructure.adapter.out.persistence.entity.TransferJpaEntity;

public class TransferJpaEntityMapper {

    // N+1 problem possible here use FETCH JOIN in queries
    public static Transfer toDomainEntity(TransferJpaEntity entity) {
        MoneyCurrency currency = MoneyCurrency.ofCode(entity.getCurrency());

        Money amount = Money.of(entity.getAmount(), currency);

        Money feeAmount = entity.getFeeAmount() != null
                ? Money.of(entity.getFeeAmount(), currency)
                : null;

        Description description = entity.getDescription() != null
                ? new Description(entity.getDescription())
                : null;

        IdempotencyKey idempotencyKey = IdempotencyKey.from(entity.getIdempotencyKey());

        return Transfer.reconstitute(
                entity.getId(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                entity.getDebitTransaction().getId(),
                entity.getCreditTransaction().getId(),
                amount,
                entity.getCategory(),
                feeAmount,
                description,
                entity.getFeeTransaction() != null ? entity.getFeeTransaction().getId() : null,
                idempotencyKey,
                entity.getExecutedAt()
        );
    }
}
