package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/lamps")
@RequiredArgsConstructor
public class LampController {

    private final MqttService mqttService;
    private final LampService lampService;

    // Przejęcie / Dodanie lampy
    @PostMapping("/claim/{lampId}")
    public ResponseEntity<Void> claimLamp(@PathVariable String lampId, Principal principal) {
        lampService.assignLampToUser(principal.getName(), lampId);
        return ResponseEntity.ok().build();
    }

    // Wysłanie komendy (Protobuf)
    @PostMapping(value = "/{lampId}/command", consumes = "application/x-protobuf")
    public ResponseEntity<Void> sendCommand(@PathVariable String lampId,
                                            @RequestBody IotProtos.LampCommand command) {
        mqttService.sendCommand(lampId, command);
        return ResponseEntity.ok().build();
    }

    // Pobranie statusu (Mock - tutaj powinieneś czytać z bazy zaktualizowanej przez MQTT)
    @GetMapping(value = "/{lampId}/status", produces = "application/x-protobuf")
    public IotProtos.LampStatus getStatus(@PathVariable String lampId) {
        return IotProtos.LampStatus.newBuilder()
                .setIsOn(true)
                .setSensorValue(123.45)
                .build();
    }
}
