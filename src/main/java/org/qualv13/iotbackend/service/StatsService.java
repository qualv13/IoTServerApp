package org.qualv13.iotbackend.service;

import org.qualv13.iotbackend.dto.DetailedStatsDto;
import org.qualv13.iotbackend.dto.LampHistoryDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final LampRepository lampRepository;
    private final UserRepository userRepository;
    private final LampMetricRepository metricRepository;
    private final FleetRepository fleetRepository;

    // --- Endpointy ---

    public DetailedStatsDto getUserStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildDetailedStats(user.getLamps());
    }

    public DetailedStatsDto getStatsForUserByAdmin(String targetUsername) {
        return getUserStats(targetUsername);
    }

    public DetailedStatsDto getGlobalStats() {
        return buildDetailedStats(lampRepository.findAll());
    }

    public DetailedStatsDto getFleetStats(Long fleetId) {
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));
        return buildDetailedStats(fleet.getLamps());
    }

    // --- Logika budowania statystyk ---

    private DetailedStatsDto buildDetailedStats(List<Lamp> lamps) {
        long online = lamps.stream().filter(Lamp::isOn).count();
        List<String> lampIds = lamps.stream().map(Lamp::getId).toList();

        Map<String, Long> colors = lamps.stream()
                .filter(l -> l.isOn() && l.getColor() != null)
                .collect(Collectors.groupingBy(Lamp::getColor, Collectors.counting()));

        Map<String, Long> firmwares = lamps.stream()
                .filter(l -> l.getFirmwareVersion() != null)
                .collect(Collectors.groupingBy(Lamp::getFirmwareVersion, Collectors.counting()));

        Map<String, Long> modes = new HashMap<>();
        modes = lamps.stream().filter(l -> l.getActiveModeId() != null).collect(Collectors.groupingBy(l -> l.getActiveModeId().toString(), Collectors.counting()));

        double currentAvgTemp = 0.0;
        double totalWatts = 0.0;

        if (!lampIds.isEmpty()) {
            List<Double> latestTemps = metricRepository.findLatestTemperaturesForLampIds(lampIds);

            // System.out.println("Latest temps from DB: " + latestTemps);

            currentAvgTemp = latestTemps.stream()
                    .filter(java.util.Objects::nonNull) // Odrzuć nulle z bazy
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }

        for (Lamp l : lamps) {
            if (l.isOn()) {
                double brightnessFactor = (l.getBrightness() != null ? l.getBrightness() : 50) / 100.0;
                totalWatts += (brightnessFactor * 9.0);
            } else {
                totalWatts += 0.5;
            }
        }

        List<String> histLabels = new ArrayList<>();
        List<Double> histValues = new ArrayList<>();

        if (!lampIds.isEmpty()) {
            List<Object[]> historyData = metricRepository.getHourlyAverageTemperature(lampIds);

            for (Object[] row : historyData) {
                if (row[0] != null && row[1] != null) {
                    histLabels.add(row[0].toString());

                    double val = ((Number) row[1]).doubleValue();
                    histValues.add(Math.round(val * 10.0) / 10.0);
                }
            }
        }

        return DetailedStatsDto.builder()
                .totalDevices(lamps.size())
                .onlineDevices(online)
                .averageTemperature(Math.round(currentAvgTemp * 10.0) / 10.0)
                .estimatedPowerUsageWatts(Math.round(totalWatts * 10.0) / 10.0)
                .colorDistribution(colors)
                .firmwareDistribution(firmwares)
                .modeDistribution(modes)
                .historyLabels(histLabels)
                .historyValues(histValues)
                .build();
    }

    public LampHistoryDto getSingleLampHistory(String lampId) {
        List<LampMetric> metrics = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId);

        Collections.reverse(metrics);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        List<String> labels = metrics.stream()
                .map(m -> m.getTimestamp().format(formatter))
                .toList();

        List<Double> temps = metrics.stream().map(m -> {
            try {
                String tempStr = m.getTemperatures();
                if (tempStr == null || tempStr.isEmpty()) return 0.0;
                return Double.parseDouble(tempStr.split(",")[0].trim());
            } catch (Exception e) {
                return 0.0;
            }
        }).toList();

        Long uptime = metrics.isEmpty() ? 0L : metrics.get(metrics.size() - 1).getUptimeSeconds();

        return LampHistoryDto.builder()
                .labels(labels)
                .temperatures(temps)
                .currentUptime(uptime)
                .build();
    }
}
