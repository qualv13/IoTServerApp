package org.qualv13.iotbackend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DetailedStatsDto {
    // Podstawowe KPI
    private long totalDevices;
    private long onlineDevices;
    private double averageTemperature;
    private double estimatedPowerUsageWatts; // NOWOŚĆ: Szacowane zużycie prądu

    // Dane do wykresów kołowych/pączkowych
    private Map<String, Long> colorDistribution;     // Jakie kolory dominują?
    private Map<String, Long> firmwareDistribution;  // Wersje softu (ważne dla floty!)
    private Map<String, Long> modeDistribution;      // Ile lamp jest w trybie DISCO, a ile STATIC?

    // Dane do wykresów liniowych (Historia)
    private List<String> historyLabels; // Oś X (godziny)
    private List<Double> historyValues; // Oś Y (temperatura/jasność)
}