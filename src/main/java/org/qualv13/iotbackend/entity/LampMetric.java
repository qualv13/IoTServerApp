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

    private LocalDateTime timestamp; // Czas odebrania przez serwer

    private Long deviceTimestamp;    // 'ts' z wiadomości proto

    private Integer uptimeSeconds;   // Nowe pole z proto

    // Przechowamy listę temperatur jako prosty String (np. "25,26,27")
    // Żeby nie tworzyć osobnej tabeli dla kilku liczb.
    private String temperatures;

    @Column(name = "ambient_light")
    private Integer ambientLight;

    @Column(name = "ambient_noise")
    private Integer ambientNoise;
}


//package org.qualv13.iotbackend.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "\"lamp_metrics\"") // Wymuszenie nazwy (opcjonalne)
//@Data
//@NoArgsConstructor
//public class LampMetric {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private Double value; // np. odczyt czujnika
//    private LocalDateTime timestamp;
//
//    @ManyToOne
//    @JoinColumn(name = "lamp_id")
//    private Lamp lamp;
//
//    public LampMetric(Double value, Lamp lamp) {
//        this.value = value;
//        this.lamp = lamp;
//        this.timestamp = LocalDateTime.now();
//    }
//}