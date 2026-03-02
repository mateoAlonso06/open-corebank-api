package com.banking.system.auth.application.service;

import com.banking.system.auth.application.dto.command.ChangeUserPasswordCommand;
import com.banking.system.auth.application.dto.command.LoginCommand;
import com.banking.system.auth.application.dto.command.RegisterCommand;
import com.banking.system.auth.application.dto.result.LoginResult;
import com.banking.system.auth.application.dto.result.RegisterResult;
import com.banking.system.auth.application.dto.result.TwoFactorRequiredResult;
import com.banking.system.auth.application.dto.result.UserResult;
import com.banking.system.auth.application.event.publisher.UserEventPublisher;
import com.banking.system.auth.application.usecase.*;
import com.banking.system.auth.domain.exception.*;
import com.banking.system.auth.domain.model.*;
import com.banking.system.auth.application.port.out.LoginTrackingPort;
import com.banking.system.auth.domain.port.out.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class AuthService implements
        RegisterUseCase,
        LoginUseCase,
        FindUserByIdUseCase,
        ChangePasswordUseCase,
        RefreshTokenUseCase,
        LogoutUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final UserEventPublisher userEventPublisher;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;
    private final VerificationTokenRepositoryPort verificationTokenRepository;
    private final TwoFactorService twoFactorService;
    private final LoginTrackingPort loginTrackingPort;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public AuthService(
            UserRepositoryPort userRepository,
            RoleRepositoryPort roleRepository,
            UserEventPublisher userEventPublisher,
            PasswordHasher passwordHasher,
            TokenGenerator tokenGenerator,
            VerificationTokenRepositoryPort verificationTokenRepository,
            @Lazy TwoFactorService twoFactorService,
            LoginTrackingPort loginTrackingPort,
            RefreshTokenRepositoryPort refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userEventPublisher = userEventPublisher;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.verificationTokenRepository = verificationTokenRepository;
        this.twoFactorService = twoFactorService;
        this.loginTrackingPort = loginTrackingPort;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserNotFoundException("Invalid credentials"));

        // for security verify password is the first check
        if (!passwordHasher.verify(command.password(), user.getPassword().value()))
            throw new InvalidCredentialsException("Invalid credentials");

        if (user.getStatus() == UserStatus.BLOCKED)
            throw new UserIsLockedException("User account is blocked");

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION)
            throw new LoginAuthenticationAccessException("Please verify your email before logging in");

        // Check if 2FA is enabled
        if (user.isTwoFactorEnabled()) {
            log.info("2FA is enabled for user: {}. Generating 2FA code.", user.getId());
            TwoFactorRequiredResult twoFactorData = twoFactorService.createTwoFactorCode(user);
            return LoginResult.withTwoFactorRequired(
                    user.getId(),
                    user.getEmail().value(),
                    twoFactorData
            );
        }

        Instant previousLogin = loginTrackingPort.registerLogin(user.getId());

        Role role = user.getRole();
        String accessToken = tokenGenerator.generateToken(
                user.getId(),
                role.getName().name()
        );

        // Single-session policy: revoke any existing sessions before issuing a new one
        refreshTokenRepository.revokeAllByUserId(user.getId());

        RefreshToken refreshToken = RefreshToken.createNew(user.getId());
        refreshTokenRepository.save(refreshToken);

        return LoginResult.withToken(
                user.getId(),
                user.getEmail().value(),
                role.getName(),
                accessToken,
                refreshToken.getToken(),
                previousLogin
        );
    }

    @Override
    @Transactional
    public LoginResult refresh(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked())
            throw new InvalidRefreshTokenException("Refresh token has been revoked");

        if (refreshToken.isExpired())
            throw new InvalidRefreshTokenException("Refresh token has expired");

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Rotation: revoke the used token and issue a new one
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        Role role = user.getRole();
        String newAccessToken = tokenGenerator.generateToken(
                user.getId(),
                role.getName().name()
        );

        RefreshToken newRefreshToken = RefreshToken.createNew(user.getId());
        refreshTokenRepository.save(newRefreshToken);

        log.info("Refresh token rotated for user: {}", user.getId());

        return LoginResult.withToken(
                user.getId(),
                user.getEmail().value(),
                role.getName(),
                newAccessToken,
                newRefreshToken.getToken(), // not serialized
                null
        );
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        // If the token no longer exists (already cleaned up by the scheduled job),
        // the session is already terminated — treat it as a successful logout.
        refreshTokenRepository.findByToken(rawToken).ifPresent(refreshToken -> {
            if (!refreshToken.isRevoked()) {
                refreshToken.revoke();
                refreshTokenRepository.save(refreshToken);
                log.info("User {} logged out, refresh token revoked", refreshToken.getUserId());
            }
        });
    }

    @Override
    @Transactional
    public RegisterResult register(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("Email already in use");
        }

        // Validates password strength (length, regex...)
        Password plainPassword = Password.fromPlainPassword(command.password());
        Password hashedPassword = Password.fromHash(passwordHasher.hash(plainPassword.value()));

        // Get default role (CUSTOMER) from database
        Role defaultRole = roleRepository.getDefaultRole();

        User user = User.createNew(
                new Email(command.email()),
                hashedPassword,
                defaultRole
        );
        User savedUser = userRepository.save(user);

        userEventPublisher.publishUserRegisteredEvent(savedUser, command);

        VerificationToken verificationToken = VerificationToken.createNew(savedUser.getId());
        verificationTokenRepository.save(verificationToken);

        userEventPublisher.publishEmailVerificationRequestedEvent(
                savedUser.getId(),
                savedUser.getEmail().value(),
                verificationToken.getToken(),
                command.firstName()
        );

        return new RegisterResult(savedUser.getId(), savedUser.getEmail().value());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResult findById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return new UserResult(user.getId(), user.getEmail().value());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangeUserPasswordCommand command) {
        log.info("Changing password for user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordHasher.verify(command.oldPassword(), user.getPassword().value())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        Password hashedNewPassword = Password.fromHash(passwordHasher.hash(command.newPassword()));

        user.changePassword(hashedNewPassword);
        userRepository.save(user);

        log.info("Password changed successfully for user with ID: {}", userId);
    }
}
