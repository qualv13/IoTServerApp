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

    Double getLampMetricByLampIdOrderByTimestampDesc(String lampId);

    /**
     * Pobiera najnowszy string 'temperatures' dla każdej lampy z podanej listy ID.
     * Wybiera te metryki, których timestamp jest równy maksymalnemu timestampowi dla danego lampId.
     */
    @Query("SELECT m.temperatures FROM LampMetric m " +
            "WHERE m.lampId IN :lampIds " +
            "AND m.timestamp = (" +
            "SELECT MAX(innerM.timestamp) " +
            "FROM LampMetric innerM " +
            "WHERE innerM.lampId = m.lampId" +
            ")")
    List<Double> findLatestTemperaturesForLampIds(@Param("lampIds") List<String> lampIds);
}