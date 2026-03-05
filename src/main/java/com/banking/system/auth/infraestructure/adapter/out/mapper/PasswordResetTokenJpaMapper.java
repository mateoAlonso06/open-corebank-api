package com.banking.system.auth.infraestructure.adapter.out.mapper;

import com.banking.system.auth.domain.model.PasswordResetToken;
import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;

public class PasswordResetTokenJpaMapper {

    private PasswordResetTokenJpaMapper() {
    }

    public static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        if (entity == null) return null;

        return PasswordResetToken.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.isUsed()
        );
    }

    public static PasswordResetTokenJpaEntity toJpaEntity(PasswordResetToken token) {
        if (token == null) return null;

        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setToken(token.getToken());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsed(token.isUsed());
        return entity;
    }
}
