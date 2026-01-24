package org.qualv13.iotbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lamp_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LampAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lamp_id")
    private String lampId;

    @Column(name = "alert_code")
    private Integer alertCode; // Mapowanie z enuma AlertCauses

    @Column(name = "alert_level")
    private Integer alertLevel; // Mapowanie z enuma AlertLevels

    private String message;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "alert_id_from_device")
    private Integer alertIdFromDevice;
}