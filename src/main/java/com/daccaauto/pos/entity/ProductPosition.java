package com.daccaauto.pos.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ProductPosition {
    FRONT("Front"),
    REAR("Rear"),
    LEFT("Left"),
    RIGHT("Right"),
    FRONT_LEFT("Front Left"),
    FRONT_RIGHT("Front Right"),
    REAR_LEFT("Rear Left"),
    REAR_RIGHT("Rear Right"),
    INNER("Inner"),
    OUTER("Outer"),
    UPPER("Upper"),
    LOWER("Lower"),
    CENTER("Center");

    private final String displayName;

    ProductPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<ProductPosition> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(value);
        return Arrays.stream(values())
            .filter(position -> normalize(position.name()).equals(normalized)
                || normalize(position.displayName).equals(normalized))
            .findFirst();
    }

    private static String normalize(String value) {
        return value.trim()
            .replaceAll("[^A-Za-z0-9]+", "_")
            .replaceAll("^_+|_+$", "")
            .toUpperCase(Locale.ROOT);
    }
}
