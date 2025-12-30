package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.LampMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LampMetricRepository extends JpaRepository<LampMetric, Long> {
    // Pobierz 100 ostatnich pomiarów dla lampy
    List<LampMetric> findTop100ByLampIdOrderByTimestampDesc(String lampId);
}