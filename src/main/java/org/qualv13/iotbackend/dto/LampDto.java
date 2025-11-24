package org.qualv13.iotbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LampDto {
    private String id;
    private boolean isOn;
}