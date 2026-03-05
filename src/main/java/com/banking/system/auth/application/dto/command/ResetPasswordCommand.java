package com.banking.system.auth.application.dto.command;

public record ResetPasswordCommand(String token, String newPassword) {
}
