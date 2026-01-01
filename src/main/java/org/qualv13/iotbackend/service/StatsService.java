package org.qualv13.iotbackend.service;

import org.eclipse.paho.client.mqttv3.util.Debug;
import org.qualv13.iotbackend.dto.StatsDto;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final LampRepository lampRepository;
    private final UserRepository userRepository;
    private final LampMetricRepository metricRepository;

    // Statystyki globalne (dla Admina)
    public StatsDto getGlobalStats() {
        List<Lamp> allLamps = lampRepository.findAll();
        long userCount = userRepository.count();

        return buildStats(allLamps, userCount);
    }

    // Statystyki użytkownika (dla zwykłego Usera)
    public StatsDto getUserStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pobieramy tylko lampy tego użytkownika
        List<Lamp> myLamps = user.getLamps();

        return buildStats(myLamps, 0);
    }

    private StatsDto buildStats(List<Lamp> lamps, long totalUsers) {
        long online = lamps.stream().filter(Lamp::isOn).count();
        long offline = lamps.size() - online;

        Map<String, Long> colors = lamps.stream()
                .filter(l -> l.getColor() != null)
                .collect(Collectors.groupingBy(Lamp::getColor, Collectors.counting()));

        double sumTemp = 0.0;
        int countReadings = 0;

        for (Lamp lamp : lamps) {
            // Pobieramy ostatnią metrykę dla danej lampy
            // (korzystamy z istniejącej metody findTop100...)
            List<LampMetric> metrics = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lamp.getId());

            if (!metrics.isEmpty()) {
                LampMetric latest = metrics.get(0);
                String tempStr = latest.getTemperatures(); // np. "24.5,25.0"

                if (tempStr != null && !tempStr.isEmpty()) {
                    try {
                        String[] parts = tempStr.split(",");
                        // Liczymy średnią dla tej jednej lampy (jeśli ma kilka czujników)
                        double lampSum = 0;
                        for (String t : parts) {
                            lampSum += Double.parseDouble(t);
                        }
                        double lampAvg = lampSum / parts.length;

                        // Dodajemy do globalnej sumy
                        sumTemp += lampAvg;
                        countReadings++;
                    } catch (NumberFormatException e) {
                        // Ignorujemy błędne dane
                    }
                }
            }
        }

        double finalAvg = (countReadings > 0) ? (sumTemp / countReadings) : 0.0;
        // ----------------------------------------------------

        return StatsDto.builder()
                .totalUsers(totalUsers)
                .totalLamps(lamps.size())
                .onlineLamps(online)
                .offlineLamps(offline)
                .averageTemperature(Math.round(finalAvg * 10.0) / 10.0) // Zaokrąglenie do 1 miejsca po przecinku
                .colorDistribution(colors)
                .build();
    }
}