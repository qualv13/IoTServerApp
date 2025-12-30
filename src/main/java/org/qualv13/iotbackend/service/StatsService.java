package org.qualv13.iotbackend.service;

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

        return buildStats(myLamps, 0); // 0, bo user nie widzi liczby innych userów
    }

    private StatsDto buildStats(List<Lamp> lamps, long totalUsers) {
        long online = lamps.stream().filter(Lamp::isOn).count();
        long offline = lamps.size() - online;

        Map<String, Long> colors = lamps.stream()
                .filter(l -> l.getColor() != null)
                .collect(Collectors.groupingBy(Lamp::getColor, Collectors.counting()));

        // --- OPTYMALIZACJA ---
        List<String> lampIds = lamps.stream().map(Lamp::getId).toList();
        Double dbAvg = 0.0;

        if (!lampIds.isEmpty()) {
            dbAvg = metricRepository.getAverageValueForLamps(lampIds);
        }
        double avgTemp = (dbAvg != null) ? dbAvg : 0.0;
        // ---------------------

        return StatsDto.builder()
                .totalUsers(totalUsers)
                .totalLamps(lamps.size())
                .onlineLamps(online)
                .offlineLamps(offline)
                .averageTemperature(Math.round(avgTemp * 10.0) / 10.0)
                .colorDistribution(colors)
                .build();
    }
}