package com.banking.system.auth.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class PasswordResetToken {

    private static final int EXPIRATION_MINUTES = 30;

    private final UUID id;
    private final UUID userId;
    private final String token;
    private final LocalDateTime expiresAt;
    private boolean used;

    private PasswordResetToken(UUID id, UUID userId, String token, LocalDateTime expiresAt, boolean used) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(expiresAt, "expiresAt cannot be null");

        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public static PasswordResetToken createNew(UUID userId) {
        return new PasswordResetToken(
                null,
                userId,
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES),
                false
        );
    }

    public static PasswordResetToken reconstitute(UUID id, UUID userId, String token, LocalDateTime expiresAt, boolean used) {
        return new PasswordResetToken(id, userId, token, expiresAt, used);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markUsed() {
        if (this.used) {
            throw new IllegalStateException("Password reset token has already been used");
        }
        if (isExpired()) {
            throw new IllegalStateException("Password reset token has expired");
        }
        this.used = true;
    }
}
