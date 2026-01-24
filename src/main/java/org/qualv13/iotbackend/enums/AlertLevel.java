package org.qualv13.iotbackend.enums;

public enum AlertLevel {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3),
    CRITICAL(4);

    private final int value;

    AlertLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AlertLevel fromValue(int value) {
        for (AlertLevel l : values()) {
            if (l.value == value) return l;
        }
        return INFO; // Default
    }
}