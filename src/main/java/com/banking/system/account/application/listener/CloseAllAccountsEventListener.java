package com.banking.system.account.application.listener;

import com.banking.system.account.application.usecase.CloseAllAccountsUseCase;
import com.banking.system.auth.application.event.CloseAllAccountsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CloseAllAccountsEventListener {
    private final CloseAllAccountsUseCase closeAllAcountsUseCase;

    /**
     * Listens for CloseAllAccountsEvent and triggers the use case to close all accounts for the specified user.
     * This method is executed before the transaction commits to ensure that all accounts are closed as part of the same transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(CloseAllAccountsEvent event) {
        closeAllAcountsUseCase.closeAllAccountsForCustomer(event.userId());
    }
}
