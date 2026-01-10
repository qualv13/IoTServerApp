package org.qualv13.iotbackend.service;

import org.qualv13.iotbackend.dto.DetailedStatsDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        // 1. Podstawowe liczniki
        long online = lamps.stream().filter(Lamp::isOn).count();
        List<String> lampIds = lamps.stream().map(Lamp::getId).toList();

        // 2. Dystrybucje (Kolory, Firmware, Tryby)
        Map<String, Long> colors = lamps.stream()
                .filter(l -> l.isOn() && l.getColor() != null)
                .collect(Collectors.groupingBy(Lamp::getColor, Collectors.counting()));

        Map<String, Long> firmwares = lamps.stream()
                .filter(l -> l.getFirmwareVersion() != null)
                .collect(Collectors.groupingBy(Lamp::getFirmwareVersion, Collectors.counting()));

        // TODO: Jeśli masz pole 'mode' w encji Lamp, odkomentuj to:
        Map<String, Long> modes = new HashMap<>();
        // Map<String, Long> modes = lamps.stream().collect(Collectors.groupingBy(Lamp::getMode, Collectors.counting()));

        // 3. Obliczenia KPI (Aktualna średnia temperatura i moc)
        double currentAvgTemp = 0.0;
        double totalWatts = 0.0;

        if (!lampIds.isEmpty()) {
            // A. Średnia temperatura (z najnowszych odczytów)
            List<Double> latestTemps = metricRepository.findLatestTemperaturesForLampIds(lampIds);
            currentAvgTemp = latestTemps.stream()
                    .mapToDouble(s -> {
                        try { return s; } catch (Exception e) { return 0.0; }
                    })
                    .average()
                    .orElse(0.0);
        }

        // B. Szacowana moc (Estymacja: 9W to max, 0.5W standby)
        for (Lamp l : lamps) {
            if (l.isOn()) {
                double brightnessFactor = (l.getBrightness() != null ? l.getBrightness() : 50) / 100.0;
                totalWatts += (brightnessFactor * 9.0);
            } else {
                totalWatts += 0.5;
            }
        }

        // 4. HISTORIA (Wykres liniowy)
        List<String> histLabels = new ArrayList<>();
        List<Double> histValues = new ArrayList<>();

        if (!lampIds.isEmpty()) {
            // Pobieramy zagregowane dane z SQL (godzina -> średnia)
            List<Object[]> historyData = metricRepository.getHourlyAverageTemperature(lampIds);

            for (Object[] row : historyData) {
                // row[0] to godzina (String), row[1] to średnia (Double)
                if (row[0] != null && row[1] != null) {
                    histLabels.add(row[0].toString());
                    // Zaokrąglenie do 1 miejsca po przecinku
                    double val = (Double) row[1];
                    histValues.add(Math.round(val * 10.0) / 10.0);
                }
            }
        }

        // 5. Budowanie DTO
        return DetailedStatsDto.builder()
                .totalDevices(lamps.size())
                .onlineDevices(online)
                .averageTemperature(Math.round(currentAvgTemp * 10.0) / 10.0)
                .estimatedPowerUsageWatts(Math.round(totalWatts * 10.0) / 10.0)
                .colorDistribution(colors)
                .firmwareDistribution(firmwares)
                .modeDistribution(modes) // Może być puste
                .historyLabels(histLabels) // Oś X
                .historyValues(histValues) // Oś Y
                .build();
    }

    // Metoda dla pojedynczej lampy (pozostaje bez zmian lub można użyć logiki powyżej)
    public Map<String, Object> getSingleLampHistory(String lampId) {
        // ... (stara implementacja dla modala) ...
        return Collections.emptyMap(); // Tu wstaw starą logikę jeśli potrzebujesz endpointu /lamps/{id}/history
    }
}

//package org.qualv13.iotbackend.service;
//
//import org.qualv13.iotbackend.dto.DetailedStatsDto;
//import org.qualv13.iotbackend.entity.Fleet;
//import org.qualv13.iotbackend.entity.Lamp;
//import org.qualv13.iotbackend.entity.LampMetric;
//import org.qualv13.iotbackend.entity.User;
//import org.qualv13.iotbackend.repository.FleetRepository;
//import org.qualv13.iotbackend.repository.LampMetricRepository;
//import org.qualv13.iotbackend.repository.LampRepository;
//import org.qualv13.iotbackend.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class StatsService {
//
//    private final LampRepository lampRepository;
//    private final UserRepository userRepository;
//    private final LampMetricRepository metricRepository;
//    private final FleetRepository fleetRepository;
//
//    // 1. Statystyki dla konkretnej Floty
//    public DetailedStatsDto getFleetStats(Long fleetId) {
//        Fleet fleet = fleetRepository.findById(fleetId)
//                .orElseThrow(() -> new RuntimeException("Fleet not found"));
//        return buildDetailedStats(fleet.getLamps());
//    }
//
//    // 2. Statystyki Admina dla konkretnego Użytkownika
//    public DetailedStatsDto getStatsForUserByAdmin(String targetUsername) {
//        User user = userRepository.findByUsername(targetUsername)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        return buildDetailedStats(user.getLamps());
//    }
//
//    // --- NAPRAWIONE: Brakująca metoda dla zwykłego usera (/stats/me) ---
//    public DetailedStatsDto getUserStats(String username) {
//        // To samo co dla admina, ale wywoływane przez usera dla siebie
//        return getStatsForUserByAdmin(username);
//    }
//    // ------------------------------------------------------------------
//
//    // 3. Statystyki globalne
//    public DetailedStatsDto getGlobalStats() {
//        return buildDetailedStats(lampRepository.findAll());
//    }
//
//    // --- LOGIKA BUDOWANIA STATYSTYK ---
//    private DetailedStatsDto buildDetailedStats(List<Lamp> lamps) {
//        long online = lamps.stream().filter(Lamp::isOn).count();
//
//        // 1. Rozkład kolorów
//        Map<String, Long> colors = lamps.stream()
//                .filter(l -> l.isOn() && l.getColor() != null)
//                .collect(Collectors.groupingBy(Lamp::getColor, Collectors.counting()));
//
//        // 2. Rozkład wersji Firmware
//        Map<String, Long> firmwares = lamps.stream()
//                .filter(l -> l.getFirmwareVersion() != null)
//                .collect(Collectors.groupingBy(Lamp::getFirmwareVersion, Collectors.counting()));
//
//        // 3. Obliczenia na bazie metryk
//        double sumTemp = 0.0;
//        double totalEstimatedWatts = 0.0;
//        int countReadings = 0;
//
//        List<String> lampIds = lamps.stream().map(Lamp::getId).toList();
//
//        if (!lampIds.isEmpty()) {
//            // Upewnij się, że masz metodę findLatestTemperatures w repozytorium!
//            // Jeśli nie, użyj starej logiki lub dodaj metodę do repo.
//            try {
//                List<Double> latestTemps = metricRepository.findLatestTemperaturesForLampIds(lampIds);
//                for (Double tStr : latestTemps) {
//                    try {
//                        sumTemp += tStr;
//                        countReadings++;
//                    } catch (NumberFormatException ignored) {}
//                }
//            } catch (Exception e) {
//                // Fallback jeśli metoda repozytorium nie istnieje/błąd
//            }
//        }
//
//        // Szacowanie mocy
//        for (Lamp l : lamps) {
//            if (l.isOn()) {
//                double brightnessPercent = (l.getBrightness() != null ? l.getBrightness() : 50) / 100.0;
//                totalEstimatedWatts += (brightnessPercent * 9.0);
//            } else {
//                totalEstimatedWatts += 0.5; // Standby
//            }
//        }
//
//        double finalAvgTemp = (countReadings > 0) ? (sumTemp / countReadings) : 0.0;
//
//        return DetailedStatsDto.builder()
//                .totalDevices(lamps.size())
//                .onlineDevices(online)
//                .averageTemperature(Math.round(finalAvgTemp * 10.0) / 10.0)
//                .estimatedPowerUsageWatts(Math.round(totalEstimatedWatts * 10.0) / 10.0)
//                .colorDistribution(colors)
//                .firmwareDistribution(firmwares)
//                .build();
//    }
//
//    // 4. Historia pojedynczej lampy
//    public Map<String, Object> getSingleLampHistory(String lampId) {
//        List<LampMetric> metrics = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId);
//        Collections.reverse(metrics);
//
//        List<String> labels = metrics.stream()
//                .map(m -> m.getTimestamp().toLocalTime().toString().substring(0, 5))
//                .toList();
//
//        List<Double> temps = metrics.stream().map(m -> {
//            try { return Double.parseDouble(m.getTemperatures()); } catch (Exception e) { return 0.0; }
//        }).toList();
//
//        return Map.of(
//                "labels", labels,
//                "temperatures", temps,
//                "uptime", metrics.isEmpty() ? 0 : metrics.get(metrics.size()-1).getUptimeSeconds()
//        );
//    }
//}

//package org.qualv13.iotbackend.service;
//
//import org.eclipse.paho.client.mqttv3.util.Debug;
//import org.qualv13.iotbackend.dto.StatsDto;
//import org.qualv13.iotbackend.entity.Lamp;
//import org.qualv13.iotbackend.entity.LampMetric;
//import org.qualv13.iotbackend.entity.User;
//import org.qualv13.iotbackend.repository.FleetRepository;
//import org.qualv13.iotbackend.repository.LampMetricRepository;
//import org.qualv13.iotbackend.repository.LampRepository;
//import org.qualv13.iotbackend.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class StatsService {
//
//    private final FleetRepository fleetRepository;
//    private final LampRepository lampRepository;
//    private final UserRepository userRepository;
//    private final LampMetricRepository metricRepository;
//
//    // Statystyki globalne (dla Admina)
//    public StatsDto getGlobalStats() {
//        List<Lamp> allLamps = lampRepository.findAll();
//        long userCount = userRepository.count();
//
//        return buildStats(allLamps, userCount);
//    }
//
//    // Statystyki użytkownika (dla zwykłego Usera)
//    public StatsDto getUserStats(String username) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // Pobieramy tylko lampy tego użytkownika
//        List<Lamp> myLamps = user.getLamps();
//
//        return buildStats(myLamps, 0);
//    }
//
//    private StatsDto buildStats(List<Lamp> lamps, long totalUsers) {
//        long online = lamps.stream().filter(Lamp::isOn).count();
//        long offline = lamps.size() - online;
//
//        Map<String, Long> colors = lamps.stream()
//                .filter(l -> l.getColor() != null)
//                .collect(Collectors.groupingBy(Lamp::getColor, Collectors.counting()));
//
//        double sumTemp = 0.0;
//        int countReadings = 0;
//
//        for (Lamp lamp : lamps) {
//            // Pobieramy ostatnią metrykę dla danej lampy
//            // (korzystamy z istniejącej metody findTop100...)
//            List<LampMetric> metrics = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lamp.getId());
//
//            if (!metrics.isEmpty()) {
//                LampMetric latest = metrics.get(0);
//                String tempStr = latest.getTemperatures(); // np. "24.5,25.0"
//
//                if (tempStr != null && !tempStr.isEmpty()) {
//                    try {
//                        String[] parts = tempStr.split(",");
//                        // Liczymy średnią dla tej jednej lampy (jeśli ma kilka czujników)
//                        double lampSum = 0;
//                        for (String t : parts) {
//                            lampSum += Double.parseDouble(t);
//                        }
//                        double lampAvg = lampSum / parts.length;
//
//                        // Dodajemy do globalnej sumy
//                        sumTemp += lampAvg;
//                        countReadings++;
//                    } catch (NumberFormatException e) {
//                        // Ignorujemy błędne dane
//                    }
//                }
//            }
//        }
//
//        double finalAvg = (countReadings > 0) ? (sumTemp / countReadings) : 0.0;
//        // ----------------------------------------------------
//
//        return StatsDto.builder()
//                .totalUsers(totalUsers)
//                .totalLamps(lamps.size())
//                .onlineLamps(online)
//                .offlineLamps(offline)
//                .averageTemperature(Math.round(finalAvg * 10.0) / 10.0) // Zaokrąglenie do 1 miejsca po przecinku
//                .colorDistribution(colors)
//                .build();
//    }
//}