package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.FirmwareRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirmwareRepository extends JpaRepository<FirmwareRelease, Long> {

    Optional<FirmwareRelease> findTopByIsPublishedTrueOrderByCreatedAtDesc();

    List<FirmwareRelease> findAllByOrderByCreatedAtDesc();
}
