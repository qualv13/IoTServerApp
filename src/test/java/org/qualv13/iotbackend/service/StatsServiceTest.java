package org.qualv13.iotbackend.service;

import org.qualv13.iotbackend.dto.StatsDto;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private LampRepository lampRepository;
    @Mock private UserRepository userRepository;
    @Mock private LampMetricRepository metricRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void shouldCalculateAverageTemperatureCorrectly() {
        // given
        String username = "testuser";
        User user = new User();
        user.setUsername(username);

        Lamp lamp1 = new Lamp(); lamp1.setId("lamp1"); lamp1.setOn(true);
        Lamp lamp2 = new Lamp(); lamp2.setId("lamp2"); lamp2.setOn(false);
        user.setLamps(List.of(lamp1, lamp2));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Mockujemy metryki
        // Lampa 1: ma 2 czujniki, 20 i 30 stopni (średnia 25)
        LampMetric metric1 = new LampMetric();
        metric1.setTemperatures("20.0,30.0");
        when(metricRepository.findTop100ByLampIdOrderByTimestampDesc("lamp1"))
                .thenReturn(List.of(metric1));

        // Lampa 2: ma 1 czujnik, 15 stopni (średnia 15)
        LampMetric metric2 = new LampMetric();
        metric2.setTemperatures("15.0");
        when(metricRepository.findTop100ByLampIdOrderByTimestampDesc("lamp2"))
                .thenReturn(List.of(metric2));

        // when
        StatsDto stats = statsService.getUserStats(username);

        // then
        // Oczekiwana średnia globalna: (25 + 15) / 2 = 20.0
        assertThat(stats.getTotalLamps()).isEqualTo(2);
        assertThat(stats.getOnlineLamps()).isEqualTo(1);
        assertThat(stats.getOfflineLamps()).isEqualTo(1);
        assertThat(stats.getAverageTemperature()).isEqualTo(20.0);
    }
}