package org.qualv13.iotbackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LampModeConfig {
    private int modeId;      // ID trybu (np. 0-9)
    private String name;     // Np. "Impreza", "Praca", "Noc"
    private String type;     // "DISCO", "SCHEDULE", "PRESET"

    // Wypełnione jest tylko jedno z poniższych (zależnie od type)
    private DiscoConfig disco;
    private ScheduleConfig schedule;
    private PresetConfig presets;

    // --- Klasy wewnętrzne ---

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
        // Wyzwalacz (Trigger)
        private String triggerType; // "HOUR", "BRIGHTNESS"

        // Pola dla Triggera
        private Integer startHour;
        private Integer endHour;
        private Integer transitionDurationMinutes;
        private Integer minBrightness;
        private Integer maxBrightness;

        // Akcja (Action)
        private String actionType; // "DIRECT", "DISCO", "PHOTO_WHITE", "PHOTO_COLOR"

        // Pola dla Akcji - Direct
        private Integer red;
        private Integer green;
        private Integer blue;
        private Integer warmWhite;
        private Integer coldWhite;

        // Pola dla Akcji - Disco
        private String discoMode;
        private Integer speed;
        private Integer intensity;

        // Pola dla Akcji - Photo
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
        private Integer coldWhite; // Dla PhotoWhiteSetting ma to pole 'intensity' i 'temperature'

        private Integer intensity;
        private Integer temperature;
        private Integer hue;
        private Integer saturation;
    }
}