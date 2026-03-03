package com.banking.system.auth.application.dto.result;

import com.banking.system.auth.domain.model.enums.RoleName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

public record LoginResult(
        UUID id,
        String email,
        RoleName role,
        String token,
        @JsonIgnore String refreshToken,
        boolean requiresTwoFactor,
        TwoFactorRequiredResult twoFactorData,
        Instant lastLoginAt
) {
    public static LoginResult withToken(UUID id, String email, RoleName role, String token, String refreshToken, Instant lastLoginAt) {
        return new LoginResult(id, email, role, token, refreshToken, false, null, lastLoginAt);
    }

    public static LoginResult withTwoFactorRequired(UUID id, String email, TwoFactorRequiredResult twoFactorData) {
        return new LoginResult(id, email, null, null, null, true, twoFactorData, null);
    }
}
