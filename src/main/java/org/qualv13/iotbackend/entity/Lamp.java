package org.qualv13.iotbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lamps")
@Data
public class Lamp {
    @Id
    private String id; // Serial Number / MAC Address

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "fleet_id")
    private Fleet fleet;

    // Przechowujemy ostatnią konfigurację w JSON lub polach prostych
    private Integer readingFrequency;
}
