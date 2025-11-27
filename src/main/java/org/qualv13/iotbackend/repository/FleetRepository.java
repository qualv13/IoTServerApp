package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.Fleet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FleetRepository extends JpaRepository<Fleet, Long> {
    // Additional: method for searching by name or user of fleet
}