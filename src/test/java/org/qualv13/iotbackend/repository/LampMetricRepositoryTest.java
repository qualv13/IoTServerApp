package org.qualv13.iotbackend.repository;

import org.junit.jupiter.api.Test;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LampMetricRepositoryTest {

    @Autowired private LampMetricRepository metricRepository;
    @Autowired private LampRepository lampRepository;

    @Test
    void shouldReturnOrderedMetrics() {
        // Given
        Lamp lamp = new Lamp();
        lamp.setId("test_lamp");
        lampRepository.save(lamp);

        LampMetric m1 = new LampMetric(10.0, lamp);
        m1.setTimestamp(LocalDateTime.now().minusHours(1));

        LampMetric m2 = new LampMetric(20.0, lamp);
        m2.setTimestamp(LocalDateTime.now()); // Najnowsze

        metricRepository.save(m1);
        metricRepository.save(m2);

        // When
        List<LampMetric> metrics = metricRepository.findTop100ByLampIdOrderByTimestampDesc("test_lamp");

        // Then
        assertEquals(2, metrics.size());
        assertEquals(20.0, metrics.get(0).getValue()); // M2 powinno być pierwsze (najnowsze)
    }
}