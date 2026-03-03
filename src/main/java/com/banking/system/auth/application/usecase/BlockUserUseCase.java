package com.banking.system.auth.application.usecase;

import java.util.UUID;

public interface BlockUserUseCase {
    void blockUser(UUID targetUserId);
    void unblockUser(UUID targetUserId);
}
