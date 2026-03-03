package com.banking.system.transaction.domain.model.value_object;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a unique transaction reference number.
 * Format: TXN-YYYYMMDD-HHMMSS-XXXX
 * Where XXXX is a random alphanumeric suffix for uniqueness.
 */
public record ReferenceNumber(String value) {

    private static final Pattern FORMAT_PATTERN = Pattern.compile(
            "^TXN-\\d{8}-\\d{6}-[A-Z0-9]{4}$"
    );

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss").withZone(ZoneOffset.UTC);

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SUFFIX_LENGTH = 4;

    public ReferenceNumber {
        Objects.requireNonNull(value, "Reference number cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Reference number cannot be blank");
        }

        if (!FORMAT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid reference number format. Expected: TXN-YYYYMMDD-HHMMSS-XXXX, got: " + value
            );
        }
    }

    /**
     * Generates a new unique reference number.
     * Uses current UTC timestamp plus a random suffix to ensure uniqueness.
     */
    public static ReferenceNumber generate() {
        Instant now = Instant.now();
        String date = DATE_FORMATTER.format(now);
        String time = TIME_FORMATTER.format(now);
        String suffix = generateRandomSuffix();

        return new ReferenceNumber("TXN-" + date + "-" + time + "-" + suffix);
    }

    private static String generateRandomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return value;
    }
}
