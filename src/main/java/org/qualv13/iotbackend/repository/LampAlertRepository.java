package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.LampAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LampAlertRepository extends JpaRepository<LampAlert, Long> {
    List<LampAlert> findByLampIdAndIsActiveTrue(String lampId);
    void deleteByLampId(String lampId);
}