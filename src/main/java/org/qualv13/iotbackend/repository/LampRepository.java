package org.qualv13.iotbackend.repository;

import org.qualv13.iotbackend.entity.Lamp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LampRepository extends JpaRepository<Lamp, String> { }