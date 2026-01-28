package org.qualv13.iotbackend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DetailedStatsDto {
    private long totalDevices;
    private long onlineDevices;
    private double averageTemperature;
    private double estimatedPowerUsageWatts;

    private Map<String, Long> colorDistribution;
    private Map<String, Long> firmwareDistribution;
    private Map<String, Long> modeDistribution;

    private List<String> historyLabels; // Oś X (godziny)
    private List<Double> historyValues; // Oś Y (temperatura/jasność)
}