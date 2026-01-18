package org.qualv13.iotbackend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LampHistoryDto {
    private List<String> labels;      // Oś X (godziny)
    private List<Double> temperatures; // Oś Y (wartości)
    private Long currentUptime;       // Dodatkowe info
}