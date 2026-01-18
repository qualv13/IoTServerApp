package org.qualv13.iotbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SmartConfigDto {
    private Boolean circadian;
    private Boolean adaptive;
}
