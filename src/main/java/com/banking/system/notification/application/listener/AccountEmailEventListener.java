package com.banking.system.notification.application.listener;

import com.banking.system.account.application.event.AccountCreatedEvent;
import com.banking.system.auth.domain.model.User;
import com.banking.system.auth.domain.port.out.UserRepositoryPort;
import com.banking.system.customer.domain.model.Customer;
import com.banking.system.customer.domain.port.out.CustomerRepositoryPort;
import com.banking.system.notification.application.service.AccountEmailService;
import com.banking.system.notification.domain.model.EmailNotification;
import com.banking.system.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEmailEventListener {

    private final AccountEmailService accountEmailService;
    private final UserRepositoryPort userRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AccountCreatedEvent event) {
        log.info("Received AccountCreatedEvent for accountNumber: {}", event.accountNumber());

        User user = userRepositoryPort.findById(event.userId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + event.userId()));

        Customer customer = customerRepositoryPort.findById(event.customerId())
                .orElseThrow(() -> new IllegalStateException("Customer not found: " + event.customerId()));

        String fullName = customer.getPersonName().fullName();

        accountEmailService.sendEmail(
                new EmailNotification(
                        user.getEmail().value(),
                        NotificationType.ACCOUNT_CREATED.getDefaultSubject(),
                        NotificationType.ACCOUNT_CREATED.getTemplateName(),
                        Map.of(
                                "customerName", fullName,
                                "accountNumber", event.accountNumber(),
                                "accountAlias", event.alias(),
                                "currency", event.currency(),
                                "accountType", event.accountType(),
                                "openedAt", event.openedAt(),
                                "balance", "0.00",
                                "loginUrl", "https://app.open-corebank.xyz/accounts"
                        ),
                        NotificationType.ACCOUNT_CREATED
                )
        );
    }
}
