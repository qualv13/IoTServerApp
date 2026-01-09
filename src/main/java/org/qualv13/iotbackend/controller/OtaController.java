package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
public class OtaController {

    private final MqttService mqttService;

    // Przykład endpointu do aktualizacji pojedynczej lampy
    @PostMapping("/lamps/{lampId}")
    public ResponseEntity<Void> updateLamp(@PathVariable String lampId, @RequestParam String url) {

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
    public ResponseEntity<Void> updateFleet(@PathVariable Long fleetId, @RequestParam String url) {

        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
                .setOtaUrl(url)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(Instant.now().getEpochSecond())
                .setDownloadOtaUpdateCommand(otaCmd)
                .build();

        mqttService.sendCommandToFleet(fleetId, command);

        return ResponseEntity.ok().build();
    }
}