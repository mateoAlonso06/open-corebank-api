package com.banking.system.auth.domain.model.value_object;

import com.banking.system.auth.domain.exception.PasswordValidationException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a password in either plain or hashed state.
 * <p>
 * Two mutually exclusive states:
 * - Plain password: for creation/change (validated for strength)
 * - Hashed password: for persistence/verification (already validated)
 * <p>
 * Security: Plain password should be hashed immediately and never stored.
 */
public record Password(String plainPassword, String hashedPassword) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    // At least one uppercase letter
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    // At least one lowercase letter
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    // At least one digit
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    // At least one special character
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    // No whitespace allowed
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s.*");

    /**
     * Compact constructor - validates state coherence.
     * Use factory methods for creation.
     */
    public Password {
        // Must have exactly one: plain OR hashed (not both, not neither)
        if (plainPassword == null && hashedPassword == null) {
            throw new PasswordValidationException("Password must have either plain or hashed value");
        }
        if (plainPassword != null && hashedPassword != null) {
            throw new PasswordValidationException("Password cannot have both plain and hashed values simultaneously");
        }

        // Validate plain password strength if present
        if (plainPassword != null) {
            validateStrength(plainPassword);
        }

        // Validate hashed password is not blank if present
        if (hashedPassword != null && hashedPassword.isBlank()) {
            throw new PasswordValidationException("Hashed password cannot be blank");
        }
    }

    /**
     * Creates a Password from plain text. Validates strength requirements.
     * The password must be hashed before persistence.
     *
     * @param plainPassword the plain text password
     * @return Password instance with plain password set
     * @throws IllegalArgumentException if password doesn't meet strength requirements
     */
    public static Password fromPlainPassword(String plainPassword) {
        return new Password(plainPassword, null);
    }

    /**
     * Reconstitutes a Password from its hashed form (from database).
     * No strength validation - assumes it was validated at creation time.
     *
     * @param hashedPassword the BCrypt hashed password
     * @return Password instance with hashed password set
     */
    public static Password fromHash(String hashedPassword) {
        return new Password(null, hashedPassword);
    }

    /**
     * Validates password strength requirements for banking security.
     */
    private static void validateStrength(String password) {
        Objects.requireNonNull(password, "Password cannot be null");

        if (password.length() < MIN_LENGTH) {
            throw new PasswordValidationException(
                    "Password must be at least " + MIN_LENGTH + " characters long"
            );
        }

        if (password.length() > MAX_LENGTH) {
            throw new PasswordValidationException(
                    "Password cannot exceed " + MAX_LENGTH + " characters"
            );
        }

        if (WHITESPACE_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException("Password cannot contain whitespace");
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException("Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException("Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException("Password must contain at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\"\\,.<>/?)");
        }
    }

    /**
     * @return true if this password is in plain text form (needs hashing)
     */
    public boolean isPlain() {
        return plainPassword != null;
    }

    /**
     * @return true if this password is already hashed (ready for persistence/verification)
     */
    public boolean isHashed() {
        return hashedPassword != null;
    }

    /**
     * Returns the appropriate value based on current state.
     * Useful for persistence adapters.
     */
    public String value() {
        return isHashed() ? hashedPassword : plainPassword;
    }

    /**
     * Security: Never expose password in toString()
     */
    @Override
    public String toString() {
        return "Password[" + (isHashed() ? "hashed" : "plain") + "]";
    }
}
