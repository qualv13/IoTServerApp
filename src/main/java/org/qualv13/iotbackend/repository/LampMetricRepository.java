package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.LampMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LampMetricRepository extends JpaRepository<LampMetric, Long> {

    Optional<LampMetric> findTopByLampIdOrderByTimestampDesc(String lampId);
    List<LampMetric> findTop100ByLampIdOrderByTimestampDesc(String lampId);
    Optional<LampMetric> findFirstByLampIdOrderByTimestampDesc(String lampId);
    void deleteByLampId(String lampId);

    Double getLampMetricByLampIdOrderByTimestampDesc(String lampId);

    @Query(value = """
        SELECT CAST(NULLIF(SPLIT_PART(m.temperatures, ',', 1), '') AS DOUBLE PRECISION)
        FROM lamp_metrics m
        WHERE m.id IN (
            SELECT MAX(id) 
            FROM lamp_metrics 
            WHERE lamp_id IN :lampIds 
            GROUP BY lamp_id
        )
        """, nativeQuery = true)
    List<Double> findLatestTemperaturesForLampIds(@Param("lampIds") List<String> lampIds);

    @Query("SELECT m.temperatures FROM LampMetric m " +
                  "WHERE m.lampId IN :lampIds " +
                  "AND m.timestamp = (" +
                  "SELECT MAX(sub.timestamp) " +
                  "FROM LampMetric sub " +
                  "WHERE sub.lampId = m.lampId" +
                  ")")
    List<String> findLatestTemperatures(@Param("lampIds") List<String> lampIds);

    @Query(value = """
        SELECT to_char(m.timestamp, 'HH24:00') as hour_bucket, 
            AVG(CAST(NULLIF(SPLIT_PART(m.temperatures, ',', 1), '') AS DOUBLE PRECISION)) as avg_temp        
        FROM lamp_metrics m
        WHERE m.lamp_id IN :lampIds
          AND m.timestamp >= NOW() - INTERVAL '24 HOURS'
          AND CAST(NULLIF(SPLIT_PART(m.temperatures, ',', 1), '') AS DOUBLE PRECISION) > -20 
          AND m.is_abnormal = false
        GROUP BY hour_bucket
        ORDER BY hour_bucket
        """, nativeQuery = true)
    List<Object[]> getHourlyAverageTemperature(@Param("lampIds") List<String> lampIds);

    List<LampMetric> findTop50ByLampIdOrderByTimestampDesc(String lampId);
}