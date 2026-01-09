package org.qualv13.iotbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LampDto {
    private String id;

    @JsonProperty("isOn")
    private boolean isOn;
    private Long fleetId;
}