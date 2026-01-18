package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
public class OtaController {

    private final MqttService mqttService;
    private final LampRepository lampRepository;

    // Przykład endpointu do aktualizacji pojedynczej lampy
    @PostMapping("/lamps/{lampId}")
    public ResponseEntity<Void> updateLamp(@PathVariable String lampId, @RequestParam String url, Authentication auth) {
        log.info("POST /ota/lamps/{} for url {}", lampId, url);
        
        // Admin only check
        if (!"admin".equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        
        // 1. Budujemy komendę OTA
        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
                .setOtaUrl(url)
                .build();

        // 2. Pakujemy w LampCommand (używając oneof)
        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(Instant.now().getEpochSecond())
                .setDownloadOtaUpdateCommand(otaCmd)
                .build();

        // 3. Wysyłamy
        mqttService.sendCommandToLamp(lampId, command);

        return ResponseEntity.ok().build();
    }

    // Przykład endpointu do aktualizacji FLOTY
    @PostMapping("/fleets/{fleetId}")
    public ResponseEntity<Void> updateFleet(@PathVariable Long fleetId, @RequestParam String url, Authentication auth) {
        log.info("POST /ota/fleets/{} for url {}", fleetId, url);
        
        // Admin only check
        if (!"admin".equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        
        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
                .setOtaUrl(url)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(Instant.now().getEpochSecond())
                .setDownloadOtaUpdateCommand(otaCmd)
                .build();

        lampRepository.findAll().forEach(lamp -> {
            mqttService.sendCommandToLamp(lamp.getId(), command);
        });
        //mqttService.sendCommandToFleet(fleetId, command);

        return ResponseEntity.ok().build();
    }

    // Nowy endpoint: OTA dla wszystkich lamp (bez floty)
    @PostMapping("/lamps")
    public ResponseEntity<Void> updateAllLamps(@RequestParam String url, Authentication auth) {
        log.info("POST /ota/lamps (ALL) for url {}", url);

        // Admin only check
        if (!"admin".equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }

        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
                .setOtaUrl(url)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(Instant.now().getEpochSecond())
                .setDownloadOtaUpdateCommand(otaCmd)
                .build();

        lampRepository.findAll().forEach(lamp -> mqttService.sendCommandToLamp(lamp.getId(), command));

        return ResponseEntity.ok().build();
    }
}