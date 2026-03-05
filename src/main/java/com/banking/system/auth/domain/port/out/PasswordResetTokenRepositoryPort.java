package com.banking.system.auth.domain.port.out;

import com.banking.system.auth.domain.model.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByToken(String token);
}
