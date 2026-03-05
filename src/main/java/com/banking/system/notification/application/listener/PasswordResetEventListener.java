package com.banking.system.notification.application.listener;

import com.banking.system.auth.application.event.PasswordResetRequestedEvent;
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
public class PasswordResetEventListener {

    private final AccountEmailService accountEmailService;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PasswordResetRequestedEvent event) {
        log.info("Sending password reset email to user {}", event.userId());

        accountEmailService.sendEmail(
                new EmailNotification(
                        event.email(),
                        NotificationType.PASSWORD_RESET.getDefaultSubject(),
                        NotificationType.PASSWORD_RESET.getTemplateName(),
                        Map.of("resetToken", event.token()),
                        NotificationType.PASSWORD_RESET
                )
        );
    }
}
