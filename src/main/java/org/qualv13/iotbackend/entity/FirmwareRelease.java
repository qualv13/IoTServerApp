package org.qualv13.iotbackend.entity;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;

@Entity
@Table(name = "firmware_releases")
@Data
public class FirmwareRelease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String version;     // np. "1.0.5"

    private String filename;    // np. "firmware_v1.0.5.bin"
    private String downloadUrl; // Publiczny link
    private String s3Key;       // Klucz w R2/S3

    private LocalDateTime createdAt;
    private boolean isPublished; // Czy widać tę wersję?
}