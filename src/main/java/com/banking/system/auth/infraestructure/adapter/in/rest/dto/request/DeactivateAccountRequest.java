package com.banking.system.auth.infraestructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(
        @NotBlank(message = "Password is required to confirm account deactivation")
        String password
) {
}
