package com.banking.system.auth.application.usecase;

import com.banking.system.auth.application.dto.command.ForgotPasswordCommand;

public interface ForgotPasswordUseCase {
    void forgotPassword(ForgotPasswordCommand command);
}
