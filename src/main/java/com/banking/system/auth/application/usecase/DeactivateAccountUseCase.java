package com.banking.system.auth.application.usecase;

import java.util.UUID;

public interface DeactivateAccountUseCase {
    void deactivateAccount(UUID userId, String password);
}
