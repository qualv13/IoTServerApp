package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.LampMetric;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LampMetricRepositoryTest {

    @Autowired
    private LampMetricRepository lampMetricRepository;

    @Test
    void shouldFindTop100ByLampIdOrderByTimestampDesc() {
        // given
        String lampId = "test_lamp_1";

        // Tworzymy metryki w pętli
        for (int i = 0; i < 5; i++) {
            LampMetric metric = new LampMetric();
            metric.setLampId(lampId);
            metric.setTemperatures("25.0,26.0"); // Nowe pole (String)
            metric.setUptimeSeconds(100 + i);    // Nowe pole
            metric.setTimestamp(LocalDateTime.now().minusMinutes(i));
            metric.setDeviceTimestamp(System.currentTimeMillis());

            lampMetricRepository.save(metric);
        }

        // when
        List<LampMetric> result = lampMetricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId);

        // then
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getTemperatures()).isEqualTo("25.0,26.0"); // Sprawdzamy getTemperatures()
    }
}