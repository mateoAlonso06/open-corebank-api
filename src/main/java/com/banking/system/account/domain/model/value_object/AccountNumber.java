package com.banking.system.account.domain.model.value_object;

import java.util.Objects;

/**
 * Represents a unique account number in the banking system.
 * An account number must be exactly 22 digits long and contain only numeric characters.
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>Cannot be null</li>
 *   <li>Must be exactly 22 digits long</li>
 *   <li>Must contain only numeric characters (0-9)</li>
 * </ul>
 *
 * <p>Example of a valid account number: "1234567890123456789012"</p>
 *
 * @param value the account number string
 * @throws NullPointerException if value is null
 * @throws IllegalArgumentException if value doesn't meet validation requirements
 */
public record AccountNumber(String value) {
    private static final int EXPECTED_LENGTH = 22;
    private static final String DIGIT_PATTERN = "^\\d{22}$";

    public AccountNumber {
        Objects.requireNonNull(value, "Account number cannot be null");

        if (value.length() != EXPECTED_LENGTH) {
            throw new IllegalArgumentException(
                    "Account number must be exactly " + EXPECTED_LENGTH + " digits"
            );
        }

        if (!value.matches(DIGIT_PATTERN)) {
            throw new IllegalArgumentException(
                    "Account number must contain only digits"
            );
        }
    }

    /**
     * Calculates 2-digit verifier using Modulo 11 algorithm (similar to CBU/CVU)
     */
    public static String calculateVerifierForBase(String baseNumber) {
        int[] weights = {3, 9, 7, 1}; // Modulo 11 weights pattern
        int sum = 0;

        for (int i = 0; i < baseNumber.length(); i++) {
            int digit = Character.getNumericValue(baseNumber.charAt(i));
            sum += digit * weights[i % weights.length];
        }

        int verifier = (11 - (sum % 11)) % 11;
        return String.format("%02d", verifier % 100); // 2 digits
    }
}
