package com.banking.system.common.infraestructure.exception;

import com.banking.system.auth.domain.exception.AccountDeactivatedException;
import com.banking.system.auth.domain.exception.PasswordValidationException;
import com.banking.system.auth.domain.exception.UserIsLockedException;
import com.banking.system.common.domain.exception.*;
import com.banking.system.notification.domain.exception.EmailRateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the banking system.
 * Security considerations:
 * - In production, internal exception messages are NEVER exposed to clients
 * - All responses include correlation ID for request tracking
 * - Detailed stack traces are logged server-side but never sent to clients
 * - Generic error messages prevent information disclosure attacks
 * - Logs contain full context for debugging using correlation ID
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Generic messages for production - prevent information disclosure
    private static final String MSG_RESOURCE_NOT_FOUND = "The requested resource was not found";
    private static final String MSG_RESOURCE_CONFLICT = "The operation could not be completed due to a conflict";
    private static final String MSG_AUTH_FAILED = "Authentication failed";
    private static final String MSG_ACCESS_DENIED = "You do not have permission to perform this action";
    private static final String MSG_BUSINESS_RULE = "The operation violates business rules";
    private static final String MSG_VALIDATION_FAILED = "The request contains invalid data";
    private static final String MSG_INVALID_ARGUMENT = "Invalid request parameters";
    private static final String MSG_INVALID_STATE = "Operation not allowed in current state";
    private static final String MSG_INTERNAL_ERROR = "An unexpected error occurred. Please contact support with the correlation ID";
    private static final String MSG_ACCOUNT_LOCKED = "Account is temporarily locked";
    private static final String MSG_ACCOUNT_DEACTIVATED = "Account has been deactivated. Please contact support.";
    private static final String MSG_RATE_LIMIT_EXCEEDED = "Too many requests. Please try again later";

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Checks if the application is running in production mode.
     * In production, we hide internal error details from clients.
     */
    private boolean isProduction() {
        return "prod".equalsIgnoreCase(activeProfile);
    }

    /**
     * Sanitizes error messages to prevent information disclosure in production.
     * In production: returns the generic fallback message
     * In development: returns the original detailed message for debugging
     *
     * @param originalMessage The original error message (logged but not exposed in prod)
     * @param fallbackMessage The generic message to use in production
     * @return The sanitized message
     */
    private String sanitizeMessage(String originalMessage, String fallbackMessage) {
        return isProduction() ? fallbackMessage : originalMessage;
    }

    /**
     * Handles resource not found exceptions.
     * Logs: Full details including resource type/ID for debugging
     * Response: Generic message in production to prevent enumeration attacks
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_RESOURCE_NOT_FOUND);
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", message, ex.getErrorCode());
    }

    /**
     * Handles optimistic locking failures caused by concurrent account modifications.
     * Occurs when two simultaneous operations (deposit, withdrawal) modify the same account.
     * The first operation wins; the second receives a 409 so the client can retry.
     * Logs: Full details to detect abnormal concurrency patterns.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Concurrent account modification detected [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(
                "The account was modified by another operation. Please retry.",
                MSG_RESOURCE_CONFLICT
        );
        return buildResponse(HttpStatus.CONFLICT, "Conflict", message, "CONCURRENT_MODIFICATION");
    }

    /**
     * Handles resource conflict exceptions (duplicate entries).
     * Logs: Full details for debugging duplicate detection
     * Response: Generic message in production to prevent user enumeration
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ResourceAlreadyExistsException ex) {
        log.warn("Resource conflict [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_RESOURCE_CONFLICT);
        return buildResponse(HttpStatus.CONFLICT, "Conflict", message, ex.getErrorCode());
    }

    /**
     * Handles authentication failures.
     * SECURITY: Never reveals whether username or password was wrong
     * Logs: Details for security auditing and intrusion detection
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        // Always use generic message - don't reveal if user exists or password is wrong
        String message = sanitizeMessage(ex.getMessage(), MSG_AUTH_FAILED);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", message, ex.getErrorCode());
    }

    /**
     * Handles access denied exceptions (domain exceptions).
     * Logs: Full details for security auditing
     * Response: Generic message to prevent privilege escalation reconnaissance
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_ACCESS_DENIED);
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", message, ex.getErrorCode());
    }

    /**
     * Handles Spring Security access denied exceptions (@PreAuthorize, @Secured, etc.).
     * Triggered when authenticated users lack required permissions/roles.
     * Logs: Full details for security auditing and privilege escalation attempts
     * Response: Generic message to prevent privilege escalation reconnaissance
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleSpringSecurityAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Spring Security access denied [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_ACCESS_DENIED);
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", message, "ACCESS_DENIED");
    }

    /**
     * Handles business rule violations.
     * Logs: Full details including any sensitive internal data (account IDs, amounts, etc.)
     * Response: Uses the exception's clientMessage in production (set by the domain layer),
     *           falling back to the generic message if not provided.
     *           This allows domain exceptions to control what is safe to expose to clients
     *           without leaking internal details (e.g. exact amounts, account IDs).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violation [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), ex.getClientMessage() != null ? ex.getClientMessage() : MSG_BUSINESS_RULE);
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", message, ex.getErrorCode());
    }

    /**
     * Handles infrastructure errors (database, external services, etc).
     * SECURITY CRITICAL: Never expose infrastructure details to clients
     * Logs: Full stack trace for debugging infrastructure issues
     */
    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<Map<String, Object>> handleInfrastructure(InfrastructureException ex) {
        log.error("Infrastructure error [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage(), ex);

        // Always generic - infrastructure details are highly sensitive
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", MSG_INTERNAL_ERROR, ex.getErrorCode());
    }

    /**
     * Handles @Valid validation errors from request bodies.
     * Logs: Detailed field errors for debugging
     * Response: In production, only show which fields failed without internal details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // Build detailed error list for logging
        List<String> detailedErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.warn("Validation failed [correlationId={}]: {}",
                MDC.get("correlationId"), detailedErrors);

        if (isProduction()) {
            // In production: only show field names without detailed messages
            List<String> fieldNames = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(FieldError::getField)
                    .distinct()
                    .toList();
            String message = MSG_VALIDATION_FAILED + ": " + String.join(", ", fieldNames);
            return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, "VALIDATION_FAILED");
        }

        // In dev: show full details for easier debugging
        String message = String.join("; ", detailedErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, "VALIDATION_FAILED");
    }

    /**
     * Handles password validation failures.
     * The validation message is always exposed — password rules are not sensitive information,
     * and the user needs to know exactly what to fix.
     */
    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordValidation(PasswordValidationException ex) {
        log.warn("Password validation failed [correlationId={}]: {}", MDC.get("correlationId"), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), ex.getErrorCode());
    }

    /**
     * Handles illegal argument exceptions.
     * Logs: Full details for debugging
     * Response: Generic message in production to hide validation logic
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_INVALID_ARGUMENT);
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, "INVALID_ARGUMENT");
    }

    /**
     * Handles illegal state exceptions.
     * Logs: Full details including current state for debugging
     * Response: Generic message to hide internal state machine details
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Invalid state [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_INVALID_STATE);
        return buildResponse(HttpStatus.CONFLICT, "Conflict", message, "INVALID_STATE");
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * SECURITY CRITICAL: This handler MUST NOT expose internal error details in production.
     * Exposing ex.getMessage() can reveal:
     * - SQL table/column names (SQL injection reconnaissance)
     * - Internal file paths (path traversal hints)
     * - Network topology (IPs, ports, hostnames)
     * - Technology stack details (version disclosure)
     * - Business logic internals
     * The full stack trace is logged server-side for debugging with correlation ID,
     * but clients only receive a generic message with the correlation ID for support.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error [correlationId={}]: {} - {}",
                MDC.get("correlationId"), ex.getClass().getName(), ex.getMessage(), ex);

        // NEVER expose internal exception messages in production
        String message = sanitizeMessage(ex.getMessage(), MSG_INTERNAL_ERROR);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message, "INTERNAL_ERROR");
    }

    /**
     * Handles permanently deactivated account exceptions.
     * Returns HTTP 403 Forbidden — the account is permanently deactivated, not temporarily locked.
     * The user must contact support to reactivate.
     */
    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountDeactivated(AccountDeactivatedException ex) {
        log.warn("Deactivated account login attempt [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_ACCOUNT_DEACTIVATED);
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", message, ex.getErrorCode());
    }

    /**
     * Handles locked account exceptions.
     * SECURITY: Generic message prevents timing-based account enumeration
     * Logs: Full details for security team to investigate potential attacks
     */
    @ExceptionHandler(UserIsLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(UserIsLockedException ex) {
        log.warn("Account locked [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_ACCOUNT_LOCKED);
        return buildResponse(HttpStatus.LOCKED, "Locked", message, ex.getErrorCode());
    }

    /**
     * Handles email rate limit exceeded exceptions.
     * Prevents email spam abuse by limiting email sending frequency per user.
     * Returns 429 Too Many Requests (standard HTTP status for rate limiting).
     * Logs: Email address and timestamp for abuse monitoring
     * Response: Generic message to inform user about rate limit
     */
    @ExceptionHandler(EmailRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleEmailRateLimitExceeded(EmailRateLimitExceededException ex) {
        log.warn("Email rate limit exceeded [correlationId={}]: {}",
                MDC.get("correlationId"), ex.getMessage());

        String message = sanitizeMessage(ex.getMessage(), MSG_RATE_LIMIT_EXCEEDED);
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", message, "EMAIL_RATE_LIMIT_EXCEEDED");
    }

    /**
     * Builds a standardized error response with correlation ID for tracking.
     * The correlation ID allows:
     * - Users to reference specific errors when contacting support
     * - Developers to find the exact request in logs
     * - Security team to trace suspicious activity
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String error, String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        if (errorCode != null) {
            body.put("errorCode", errorCode);
        }

        // Include correlation ID if available (set by CorrelationIdFilter)
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }

        return ResponseEntity.status(status).body(body);
    }
}
