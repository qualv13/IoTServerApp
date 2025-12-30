package org.qualv13.iotbackend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class StatsDto {
    private long totalUsers;      // Tylko dla admina
    private long totalLamps;
    private long onlineLamps;
    private long offlineLamps;

    private double averageTemperature;

    // Mapa: Kolor HEX -> Liczba lamp (np. "#ffffff" -> 10)
    private Map<String, Long> colorDistribution;
}