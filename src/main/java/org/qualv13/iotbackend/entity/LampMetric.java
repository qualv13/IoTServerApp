package org.qualv13.iotbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lamp_metrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LampMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lampId;

    private LocalDateTime timestamp;

    private Long deviceTimestamp;

    private Integer uptimeSeconds;

    private String temperatures;

    @Column(name = "ambient_light")
    private Integer ambientLight;

    @Column(name = "ambient_noise")
    private Integer ambientNoise;

    @Column(name = "is_abnormal")
    private Boolean isAbnormal = false;
}
