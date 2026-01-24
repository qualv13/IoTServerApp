package org.qualv13.iotbackend.enums;

public enum AlertCause {
    GENERAL(0),
    WIFI(1),
    OTA_UPDATE(2),
    HARDWARE(3),
    INVALID_CONFIG(4),
    INVALID_COMMAND(5),
    INVALID_MODE(6),
    INVALID_PRESET(7);

    private final int value;

    AlertCause(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AlertCause fromValue(int value) {
        for (AlertCause c : values()) {
            if (c.value == value) return c;
        }
        return GENERAL;
    }
}