package com.banking.system.auth.application.usecase;

import com.banking.system.auth.application.dto.command.ResetPasswordCommand;

public interface ResetPasswordUseCase {
    void resetPassword(ResetPasswordCommand command);
}
