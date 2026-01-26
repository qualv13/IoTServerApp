package org.qualv13.iotbackend.dto.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Pojedynczy wpis w tablicy "entries" (To co wysłałeś w przykładzie)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresetEntryJson {
    private String type; // DIRECT, WHITE, COLOR

    // Pola Direct
    private Integer red;
    private Integer green;
    private Integer blue;
    private Integer warmWhite;
    private Integer coldWhite;
    private Integer neutralWhite;

    // Pola White/Color
    private Integer intensity;
    private Integer temperature;
    private Integer hue;
    private Integer saturation;
}
