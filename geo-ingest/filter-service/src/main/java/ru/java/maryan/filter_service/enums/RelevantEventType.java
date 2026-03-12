package ru.java.maryan.filter_service.enums;

import java.util.Arrays;
import java.util.Optional;

public enum RelevantEventType {
    ATTACH(), // Подключение к сети - только что вошел в зону
    TAU(), // Обновление локации - перемещается между зонами
    LAU(), // Обновление локации - перемещается
    LOCATION_UPDATE(), // Обновление локации - явно для гео
    HANDOVER(); // Переключение между сотами - движется

    public static Optional<RelevantEventType> fromString(String event) {
        if (event == null) return Optional.empty();

        String upperEvent = event.trim().toUpperCase();

        return Arrays.stream(values())
                .filter(value -> value.name().equals(upperEvent))
                .findFirst();
    }
}
