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

    //private Integer readingFrequency;

    private Integer brightness = 50;
    private String color = "#ffffff";
    private Integer reportInterval = 60;

    // Status
    @Column(name = "is_on")
    private boolean isOn = false;
}
