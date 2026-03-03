package com.banking.system.account.application.usecase;

import java.util.UUID;

public interface CloseAllAccountsUseCase {
    void closeAllAccountsForCustomer(UUID userId);
}
