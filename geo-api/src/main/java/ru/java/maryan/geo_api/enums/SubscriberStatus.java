package ru.java.maryan.geo_api.enums;

public enum SubscriberStatus {
    OTHER(0),
    WORK(1),
    HOME(2);

    private final int code;

    SubscriberStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static SubscriberStatus fromCode(Integer code) {
        if (code == null) {
            return OTHER;
        }

        return switch (code) {
            case 1 -> WORK;
            case 2 -> HOME;
            default -> OTHER;
        };
    }
}

