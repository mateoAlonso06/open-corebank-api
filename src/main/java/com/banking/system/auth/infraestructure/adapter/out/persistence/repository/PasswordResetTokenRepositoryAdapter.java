package com.banking.system.auth.infraestructure.adapter.out.persistence.repository;

import com.banking.system.auth.domain.model.PasswordResetToken;
import com.banking.system.auth.domain.port.out.PasswordResetTokenRepositoryPort;
import com.banking.system.auth.infraestructure.adapter.out.mapper.PasswordResetTokenJpaMapper;
import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final SpringPasswordResetTokenJpaRepository springPasswordResetTokenJpaRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaMapper.toJpaEntity(token);
        PasswordResetTokenJpaEntity saved = springPasswordResetTokenJpaRepository.save(entity);
        return PasswordResetTokenJpaMapper.toDomain(saved);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return springPasswordResetTokenJpaRepository.findByToken(token)
                .map(PasswordResetTokenJpaMapper::toDomain);
    }
}
