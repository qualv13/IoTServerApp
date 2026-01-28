package org.qualv13.iotbackend.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LampModeConfig {
    private int modeId;      // ID trybu
    private String name;
    private String type;     // "DISCO", "SCHEDULE", "PRESET"

    private DiscoConfig disco;
    private ScheduleConfig schedule;
    private PresetConfig presets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscoConfig {
        private String mode; // Enum: "COLOR_CYCLE", "STROBE", "PULSE"
        private int speed;
        private int intensity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleConfig {
        private List<ScheduleEntry> entries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleEntry {
        private String triggerType; // "HOUR", "BRIGHTNESS"

        // Pola dla Triggera
        private Integer startHour;
        private Integer endHour;

        private Integer transitionDurationSeconds;

        private Integer minBrightness;
        private Integer maxBrightness;

        private String actionType; // "DIRECT", "DISCO", "PHOTO_WHITE", "PHOTO_COLOR"

        // Direct
        private Integer red;
        private Integer green;
        private Integer blue;
        private Integer warmWhite;
        private Integer coldWhite;
        private Integer neutralWhite;

        // Disco
        private String discoMode;
        private Integer speed;
        private Integer intensity;

        // Photo
        private Integer temperature;
        private Integer hue;
        private Integer saturation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresetConfig {
        private List<PresetEntry> entries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresetEntry {
        private String type; // "DIRECT", "WHITE", "COLOR"

        private Integer red;
        private Integer green;
        private Integer blue;

        private Integer warmWhite;
        private Integer coldWhite;
        private Integer neutralWhite;

        private Integer intensity;
        private Integer temperature;
        private Integer hue;
        private Integer saturation;
    }
}