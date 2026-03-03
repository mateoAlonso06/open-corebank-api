package com.banking.system.auth.infraestructure.adapter.in.rest;

import com.banking.system.auth.application.dto.command.ResendVerificationCommand;
import com.banking.system.auth.application.dto.command.VerifyEmailCommand;
import com.banking.system.auth.application.dto.result.LoginResult;
import com.banking.system.auth.application.dto.result.RegisterResult;
import com.banking.system.auth.application.dto.result.TwoFactorStatusResult;
import com.banking.system.auth.application.usecase.*;
import com.banking.system.auth.infraestructure.adapter.in.rest.dto.request.*;
import com.banking.system.auth.infraestructure.adapter.in.rest.dto.response.TwoFactorStatusResponse;
import com.banking.system.auth.infraestructure.config.CookieHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "User registration, login, and password management")
public class AuthRestController {
    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;
    private final VerifyTwoFactorUseCase verifyTwoFactorUseCase;
    private final ToggleTwoFactorUseCase toggleTwoFactorUseCase;
    private final GetTwoFactorStatusUseCase getTwoFactorStatusUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final DeactivateAccountUseCase deactivateAccountUseCase;
    private final CookieHelper cookieHelper;

    @Operation(
            summary = "Register new user",
            description = "Creates a new user account with email and password. Also creates associated customer profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "409", description = "Email already in use"),
            @ApiResponse(responseCode = "422", description = "Invalid data provided")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResult> register(@RequestBody @Valid RegisterUserRequest request) {
        var result = registerUseCase.register(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(
            summary = "User login",
            description = "Authenticates user with email and password. Returns JWT token on success. " +
                    "The refresh token is set as an HttpOnly cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account has been deactivated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "423", description = "User is blocked"),
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@RequestBody @Valid LoginRequest request) {
        var result = loginUseCase.login(request.toCommand());

        if (result.requiresTwoFactor()) {
            return ResponseEntity.ok(result);
        }

        return buildResponseWithCookies(result);
    }

    @Operation(
            summary = "Verify email",
            description = "Verifies a user's email address using the token sent during registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "422", description = "Invalid or expired verification token or user already verified")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        verifyEmailUseCase.verifyEmail(new VerifyEmailCommand(request.token()));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Resend verification email",
            description = "Resends the email verification link to the user's email address."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification email resent"),
            @ApiResponse(responseCode = "422", description = "User is already verified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        resendVerificationEmailUseCase.resendVerificationEmail(
                new ResendVerificationCommand(request.email())
        );
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Change password",
            description = "Changes the password for the authenticated user. Requires current password verification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token, or incorrect current password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid ChangeUserPasswordRequest request) {
        changePasswordUseCase.changePassword(userId, request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Verify two-factor authentication code",
            description = "Verifies the 2FA code and returns JWT token on success. " +
                    "The refresh token is set as an HttpOnly cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "2FA verification successful, JWT token returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired code"),
            @ApiResponse(responseCode = "422", description = "Maximum verification attempts exceeded")
    })
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResult> verifyTwoFactor(@RequestBody @Valid VerifyTwoFactorRequest request) {
        var result = verifyTwoFactorUseCase.verify(request.toCommand());
        return buildResponseWithCookies(result);
    }

    @Operation(
            summary = "Toggle two-factor authentication",
            description = "Enables or disables two-factor authentication for the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "2FA status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "422", description = "Cannot enable 2FA for inactive users")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/2fa/toggle")
    public ResponseEntity<TwoFactorStatusResponse> toggleTwoFactor(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid ToggleTwoFactorRequest request) {
        TwoFactorStatusResult result = toggleTwoFactorUseCase.toggle(userId, request.toCommand());
        return ResponseEntity.ok(new TwoFactorStatusResponse(result.enabled()));
    }

    @Operation(
            summary = "Get two-factor authentication status",
            description = "Returns whether 2FA is enabled for the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "2FA status returned successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/2fa/status")
    public ResponseEntity<TwoFactorStatusResponse> getTwoFactorStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        TwoFactorStatusResult result = getTwoFactorStatusUseCase.getStatus(userId);
        return ResponseEntity.ok(new TwoFactorStatusResponse(result.enabled()));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Issues a new access token using the refresh token from the HttpOnly cookie. " +
                    "The old refresh token is revoked and a new one is set as a cookie (rotation)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token returned, refresh token cookie rotated"),
            @ApiResponse(responseCode = "401", description = "Refresh token is missing, invalid, expired, or revoked"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResult> refresh(HttpServletRequest request) {
        String refreshToken = cookieHelper.extractRefreshToken(request)
                .orElse(null);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var result = refreshTokenUseCase.refresh(refreshToken);
        return buildResponseWithCookies(result);
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the refresh token from the HttpOnly cookie, ending the session. " +
                    "The access token expires naturally."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookieHelper.extractRefreshToken(request)
                .ifPresent(logoutUseCase::logout);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieHelper.clearCsrfTokenCookie().toString())
                .build();
    }

    @Operation(
            summary = "Deactivate account",
            description = "Permanently deactivates the authenticated user's account. " +
                    "Requires password confirmation. Sessions are revoked immediately. " +
                    "This action is irreversible — contact support to reactivate."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token, or incorrect password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/account")
    public ResponseEntity<Void> deactivateAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid DeactivateAccountRequest request) {
        deactivateAccountUseCase.deactivateAccount(userId, request.password());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieHelper.clearCsrfTokenCookie().toString())
                .build();
    }

    private ResponseEntity<LoginResult> buildResponseWithCookies(LoginResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHelper.createRefreshTokenCookie(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieHelper.createCsrfTokenCookie().toString())
                .body(result);
    }
}
