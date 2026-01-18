package org.qualv13.iotbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedLampDto {
    private String id;
    private String deviceName;
    private String firmwareVersion;
    
    @JsonProperty("isOn")
    private boolean isOn;
    
    @JsonProperty("isOnline")
    private boolean isOnline;
    
    private Integer brightness;
    private String color;
    private Integer reportInterval;
    private Long fleetId;
    private Integer activeModeId;
    private Double currentTemperature;
    private Long uptimeSeconds;
    private String lastUpdate;

    private Integer ambientLight;
    private Integer ambientNoise;

    private boolean isCircadianEnabled;
    private boolean isAdaptiveBrightnessEnabled;
}
