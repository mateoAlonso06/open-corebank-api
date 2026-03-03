package com.banking.system.auth.domain.model.value_object;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a validated email address.
 * Ensures email format compliance and normalization.
 */
public record Email(String value) {

    private static final int MAX_LENGTH = 254; // RFC 5321
    private static final int LOCAL_PART_MAX_LENGTH = 64; // RFC 5321

    // RFC 5322 simplified pattern - covers most valid email formats
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * Compact constructor with validation and normalization.
     */
    public Email {
        Objects.requireNonNull(value, "Email cannot be null");

        value = value.trim().toLowerCase();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Email exceeds maximum length of " + MAX_LENGTH + " characters"
            );
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid email format: " + value
            );
        }

        if (value.contains("..")) {
            throw new IllegalArgumentException("Email cannot contain consecutive dots");
        }

        String localPart = value.substring(0, value.indexOf('@'));
        if (localPart.length() > LOCAL_PART_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Email local part exceeds maximum length of " + LOCAL_PART_MAX_LENGTH + " characters"
            );
        }
    }

    /**
     * Returns masked email for logging/display purposes.
     * Example: "john.doe@example.com" → "jo***@example.com"
     */
    public String masked() {
        int atIndex = value.indexOf('@');
        String localPart = value.substring(0, atIndex);
        String domain = value.substring(atIndex);

        if (localPart.length() <= 2) {
            return "***" + domain;
        }
        return localPart.substring(0, 2) + "***" + domain;
    }

    /**
     * Returns the domain part of the email.
     */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    @Override
    public String toString() {
        return value;
    }
}
