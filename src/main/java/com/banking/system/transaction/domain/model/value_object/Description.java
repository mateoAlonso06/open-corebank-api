package com.banking.system.transaction.domain.model.value_object;

import java.util.Objects;

/**
 * Value Object representing a transaction description.
 * Ensures description is not blank and within length limits.
 */
public record Description(String value) {

    private static final int MAX_LENGTH = 255;

    public Description {
        Objects.requireNonNull(value, "Description cannot be null");

        value = value.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Description exceeds maximum length of " + MAX_LENGTH + " characters"
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
