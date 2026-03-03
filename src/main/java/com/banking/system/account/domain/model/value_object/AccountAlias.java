package com.banking.system.account.domain.model.value_object;

import java.util.regex.Pattern;

/**
 * Represents a unique alias for an account.
 * An alias must be between 6 and 20 characters long and contain only lowercase letters,
 * numbers, dots, underscores, and hyphens.
 *
 * Example of a valid alias: "john.doe_123"
 */
public record AccountAlias(String value) {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-z0-9._-]{6,20}$");

    public AccountAlias {
        if (value == null) {
            throw new IllegalArgumentException("Alias cannot be null");
        }
        if (!ALIAS_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid account alias format");
        }
    }
}
