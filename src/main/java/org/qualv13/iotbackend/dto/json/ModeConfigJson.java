package org.qualv13.iotbackend.dto.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModeConfigJson {
    private int modeId;
    private String name;
    private String type; // np. "PRESET"
    private PresetContainer presets;
}

