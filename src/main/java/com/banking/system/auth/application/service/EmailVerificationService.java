package com.banking.system.auth.application.service;

import com.banking.system.auth.application.dto.command.ResendVerificationCommand;
import com.banking.system.auth.application.dto.command.VerifyEmailCommand;
import com.banking.system.auth.application.event.publisher.UserEventPublisher;
import com.banking.system.auth.application.usecase.ResendVerificationEmailUseCase;
import com.banking.system.auth.application.usecase.VerifyEmailUseCase;
import com.banking.system.auth.domain.exception.InvalidVerificationTokenException;
import com.banking.system.auth.domain.exception.UserIsAlreadyProcessedException;
import com.banking.system.auth.domain.model.User;
import com.banking.system.auth.domain.model.enums.UserStatus;
import com.banking.system.auth.domain.model.VerificationToken;
import com.banking.system.auth.domain.port.out.UserRepositoryPort;
import com.banking.system.auth.domain.port.out.VerificationTokenRepositoryPort;
import com.banking.system.auth.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService implements VerifyEmailUseCase, ResendVerificationEmailUseCase {

    private final VerificationTokenRepositoryPort tokenRepository;
    private final UserRepositoryPort userRepository;
    private final UserEventPublisher eventPublisher;

    /**
     * Verifies the user's email using the provided verification token. If the token is valid and not expired,
     * the user's status is updated to active. If the token is invalid, expired, or already used, an exception is thrown.
     *
     * @param command The command containing the verification token.
     * @throws InvalidVerificationTokenException If the token is invalid, expired, or already used.
     * @throws UserNotFoundException If no user is found associated with the token.
     */
    @Override
    @Transactional
    public void verifyEmail(VerifyEmailCommand command) {
        log.info("Verifying email with token");

        VerificationToken token = tokenRepository.findByToken(command.token())
                .orElseThrow(() -> new InvalidVerificationTokenException("Verification token not found"));

        try {
            token.markUsed();
        } catch (IllegalStateException e) {
            throw new InvalidVerificationTokenException(e.getMessage());
        }

        tokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.activate();
        userRepository.save(user);

        log.info("Email verified successfully for user {}", user.getId());
    }

    /**
     * Resends the verification email to the user if they are still pending verification.
     * If the user is already verified or processed, an exception is thrown.
     *
     * @param command The command containing the email address to resend the verification email to.
     * @throws UserNotFoundException If no user is found with the provided email.
     * @throws UserIsAlreadyProcessedException If the user is already verified or processed.
     */
    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationCommand command) {
        log.info("Resending verification email to {}", command.email());

        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new UserIsAlreadyProcessedException("User is already verified");
        }

        VerificationToken token = VerificationToken.createNew(user.getId());
        tokenRepository.save(token);

        eventPublisher.publishEmailVerificationRequestedEvent(
                user.getId(), user.getEmail().value(), token.getToken(), null
        );

        log.info("Verification email resent for user {}", user.getId());
    }
}