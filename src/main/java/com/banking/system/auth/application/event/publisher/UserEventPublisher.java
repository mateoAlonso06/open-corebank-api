package com.banking.system.auth.application.event.publisher;

import com.banking.system.auth.application.dto.command.RegisterCommand;
import com.banking.system.auth.domain.model.User;

import java.util.UUID;

public interface UserEventPublisher {

    void publishUserRegisteredEvent(User user, RegisterCommand command);

    void publishEmailVerificationRequestedEvent(UUID userId, String email, String token, String firstName);

    void publishTwoFactorCodeRequestedEvent(UUID userId, String email, String code, String firstName);

    void publishCloseAllAccountsRequestEvent(UUID userId);
}
