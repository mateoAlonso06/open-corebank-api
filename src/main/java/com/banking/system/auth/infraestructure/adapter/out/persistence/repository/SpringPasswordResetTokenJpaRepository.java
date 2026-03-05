package com.banking.system.auth.infraestructure.adapter.out.persistence.repository;

import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringPasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {
    Optional<PasswordResetTokenJpaEntity> findByToken(String token);
}
