package com.banking.system.auth.application.service;

import com.banking.system.auth.application.usecase.BlockUserUseCase;
import com.banking.system.auth.domain.exception.UserNotFoundException;
import com.banking.system.auth.domain.model.User;
import com.banking.system.auth.domain.port.out.RefreshTokenRepositoryPort;
import com.banking.system.auth.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService implements BlockUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    @Override
    @Transactional
    public void blockUser(UUID targetUserId) {
        log.info("Blocking user with ID: {}", targetUserId);

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.block();
        refreshTokenRepository.revokeAllByUserId(targetUserId);
        userRepository.save(user);

        log.info("User {} blocked and sessions revoked", targetUserId);
    }

    @Override
    @Transactional
    public void unblockUser(UUID targetUserId) {
        log.info("Unblocking user with ID: {}", targetUserId);

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.unblock();
        userRepository.save(user);

        log.info("User {} unblocked successfully", targetUserId);
    }
}
