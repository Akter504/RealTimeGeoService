package ru.java.maryan.filter_service.enums;

import java.util.Arrays;
import java.util.Optional;

public enum RatType {
    LTE(-140),
    NR(-150),
    WCDMA(-115),
    UMTS(-115);

    private final int minSignalStrength;

    RatType(int signalStrength) {
        this.minSignalStrength = signalStrength;
    }

    public int getMinSignalStrength() {
        return minSignalStrength;
    }

    public static Optional<RatType> fromString(String rat) {
        if (rat == null) return Optional.empty();

        String upperRat = rat.trim().toUpperCase();

        return Arrays.stream(values())
                .filter(value -> value.name().equals(upperRat))
                .findFirst();
    }
}
