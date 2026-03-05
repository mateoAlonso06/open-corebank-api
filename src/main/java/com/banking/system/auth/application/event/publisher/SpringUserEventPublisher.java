package com.banking.system.auth.application.event.publisher;

import com.banking.system.auth.application.dto.command.RegisterCommand;
import com.banking.system.auth.application.event.CloseAllAccountsEvent;
import com.banking.system.auth.application.event.EmailVerificationRequestedEvent;
import com.banking.system.auth.application.event.PasswordResetRequestedEvent;
import com.banking.system.auth.application.event.TwoFactorCodeRequestedEvent;
import com.banking.system.auth.application.event.UserRegisteredEvent;
import com.banking.system.auth.domain.model.User;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringUserEventPublisher implements UserEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishUserRegisteredEvent(User user, RegisterCommand command) {
        applicationEventPublisher.publishEvent(
                new UserRegisteredEvent(
                        user.getId(),
                        command.firstName(),
                        command.lastName(),
                        command.documentType(),
                        command.documentNumber(),
                        command.birthDate(),
                        command.phone(),
                        command.address(),
                        command.city(),
                        command.country()
                )
        );
    }

    @Override
    public void publishEmailVerificationRequestedEvent(UUID userId, String email, String token, String firstName) {
        applicationEventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(userId, email, token, firstName)
        );
    }

    @Override
    public void publishTwoFactorCodeRequestedEvent(UUID userId, String email, String code, String firstName) {
        applicationEventPublisher.publishEvent(
                new TwoFactorCodeRequestedEvent(userId, email, code, firstName)
        );
    }

    @Override
    public void publishCloseAllAccountsRequestEvent(UUID userId) {
        applicationEventPublisher.publishEvent(
                new CloseAllAccountsEvent(userId)
        );
    }

    @Override
    public void publishPasswordResetRequestedEvent(UUID userId, String email, String token) {
        applicationEventPublisher.publishEvent(
                new PasswordResetRequestedEvent(userId, email, token)
        );
    }
}
