package org.qualv13.iotbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresetDto {
    private int slotIndex;
    private String name;
    private String type;
    private Map<String, Object> data; // Frontend wyśle tu JSON
}