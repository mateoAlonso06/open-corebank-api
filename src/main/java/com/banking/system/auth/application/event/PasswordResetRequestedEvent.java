package com.banking.system.auth.application.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(UUID userId, String email, String token) {
}
