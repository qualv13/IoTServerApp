package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.LampMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LampMetricRepository extends JpaRepository<LampMetric, Long> {

    List<LampMetric> findTop100ByLampIdOrderByTimestampDesc(String lampId);
    Optional<LampMetric> findFirstByLampIdOrderByTimestampDesc(String lampId);
    // NOWE: Oblicz średnią wartość dla podanej listy ID lamp
//    @Query("SELECT AVG(m.value) FROM LampMetric m WHERE m.lamp.id IN :lampIds")
//    Double getAverageValueForLamps(@Param("lampIds") List<String> lampIds);
}