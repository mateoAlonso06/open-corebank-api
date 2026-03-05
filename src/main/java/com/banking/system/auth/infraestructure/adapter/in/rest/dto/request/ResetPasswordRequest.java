package com.banking.system.auth.infraestructure.adapter.in.rest.dto.request;

import com.banking.system.auth.application.dto.command.ResetPasswordCommand;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "New password is required")
        String newPassword
) {
    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(token, newPassword);
    }
}
