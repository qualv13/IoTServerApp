package org.qualv13.iotbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lamps")
@Data
public class Lamp {
    @Id
    private String id; // Serial Number / MAC Address

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_token_hash")
    private String deviceTokenHash;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "fleet_id")
    private Fleet fleet;

    //private Integer readingFrequency;
    private String firmwareVersion;

    private Integer brightness = 50;
    private String color = "#ffffff";
    private Integer reportInterval = 60;

    private Integer red = 0;
    private Integer green = 0;
    private Integer blue = 0;

    @Column(name = "cold_white")
    private Integer coldWhite = 0;

    @Column(name = "neutral_white")
    private Integer neutralWhite = 0;

    @Column(name = "warm_white")
    private Integer warmWhite = 0;

    // --- SENSORY (Ostatni znany odczyt) ---
    @Column(name = "last_ambient_light")
    private Integer lastAmbientLight;

    @Column(name = "last_ambient_noise")
    private Integer lastAmbientNoise;

    // --- TRYB PHOTO WHITE ---
    @Column(name = "photo_white_intensity")
    private Integer photoWhiteIntensity;
    @Column(name = "photo_white_temp")
    private Integer photoWhiteTemp;

    // --- TRYB PHOTO COLOR ---
    @Column(name = "photo_color_intensity")
    private Integer photoColorIntensity;
    @Column(name = "photo_color_hue")
    private Integer photoColorHue;
    @Column(name = "photo_color_saturation")
    private Integer photoColorSaturation;

    // --- TRYB DISCO ---
    @Column(name = "disco_mode")
    private String discoMode; // Enum jako String
    @Column(name = "disco_speed")
    private Integer discoSpeed;
    @Column(name = "disco_intensity")
    private Integer discoIntensity;

    // --- SMART FEATURES FLAGS ---
    @Column(name = "is_circadian_enabled")
    private boolean isCircadianEnabled = false;

    @Column(name = "is_adaptive_brightness_enabled")
    private boolean isAdaptiveBrightnessEnabled = false;

    // Status
    @Column(name = "is_on")
    private boolean isOn = false;

    @Column(name = "is_online")
    private boolean isOnline = false;

    // Przechowujemy JSON z listą trybów (do 10 slotów)
    // TEXT pozwala na zapisanie długiego JSON-a
    @Column(columnDefinition = "TEXT")
    private String modesConfigJson;

    // Pole pomocnicze: który tryb jest teraz aktywny
    private Integer activeModeId;
}
