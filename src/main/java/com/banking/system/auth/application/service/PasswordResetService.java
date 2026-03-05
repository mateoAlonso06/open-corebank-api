package com.banking.system.auth.application.service;

import com.banking.system.auth.application.dto.command.ForgotPasswordCommand;
import com.banking.system.auth.application.dto.command.ResetPasswordCommand;
import com.banking.system.auth.application.event.publisher.UserEventPublisher;
import com.banking.system.auth.application.usecase.ForgotPasswordUseCase;
import com.banking.system.auth.application.usecase.ResetPasswordUseCase;
import com.banking.system.auth.domain.exception.InvalidPasswordResetTokenException;
import com.banking.system.auth.domain.model.PasswordResetToken;
import com.banking.system.auth.domain.model.User;
import com.banking.system.auth.domain.model.value_object.Password;
import com.banking.system.auth.domain.port.out.PasswordHasher;
import com.banking.system.auth.domain.port.out.PasswordResetTokenRepositoryPort;
import com.banking.system.auth.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService implements ForgotPasswordUseCase, ResetPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordResetTokenRepositoryPort passwordResetTokenRepository;
    private final UserEventPublisher eventPublisher;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordCommand command) {
        log.info("Processing forgot password request for email {}", command.email());

        userRepository.findByEmail(command.email()).ifPresent(user -> {
            PasswordResetToken token = PasswordResetToken.createNew(user.getId());
            passwordResetTokenRepository.save(token);

            eventPublisher.publishPasswordResetRequestedEvent(
                    user.getId(), user.getEmail().value(), token.getToken()
            );

            log.info("Password reset token created for user {}", user.getId());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        log.info("Processing password reset");

        PasswordResetToken token = passwordResetTokenRepository.findByToken(command.token())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Password reset token not found"));

        try {
            token.markUsed();
        } catch (IllegalStateException e) {
            throw new InvalidPasswordResetTokenException(e.getMessage());
        }

        passwordResetTokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("User not found"));

        Password newPassword = Password.fromPlainPassword(command.newPassword());
        String hashedValue = passwordHasher.hash(newPassword.value());
        Password hashedPassword = Password.fromHash(hashedValue);
        user.changePassword(hashedPassword);
        userRepository.save(user);

        log.info("Password reset successfully for user {}", user.getId());
    }
}
