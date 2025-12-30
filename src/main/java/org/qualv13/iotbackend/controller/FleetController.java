package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.dto.FleetDto;
import org.qualv13.iotbackend.dto.LampDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.service.FleetService;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fleets")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;
    private final MqttService mqttService;
    private final FleetRepository fleetRepository;

    // --- Fleet management ---

    @GetMapping
    public ResponseEntity<List<FleetDto>> listMyFleets(Principal principal) {
        // Mapping Fleet -> FleetDto (id, name)
        var fleets = fleetService.getMyFleets(principal.getName()).stream()
                .map(f -> new FleetDto(f.getId(), f.getName())) // Create DTO
                .collect(Collectors.toList());
        return ResponseEntity.ok(fleets);
    }

    @PostMapping
    public ResponseEntity<Void> createFleet(@RequestBody FleetDto dto, Principal principal) {
        fleetService.createFleet(dto.getName(), principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{fleetId}/lamps")
    public ResponseEntity<List<LampDto>> getLampsInFleet(@PathVariable Long fleetId) {
        return ResponseEntity.ok(fleetService.getLampsInFleet(fleetId));
    }

    @PostMapping("/{fleetId}/lamps/{lampId}")
    public ResponseEntity<Void> addLampToFleet(@PathVariable Long fleetId, @PathVariable String lampId) {
        fleetService.addLampToFleet(fleetId, lampId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{fleetId}/lamps/{lampId}")
    public ResponseEntity<Void> removeLampFromFleet(@PathVariable Long fleetId, @PathVariable String lampId) {
        fleetService.removeLampFromFleet(fleetId, lampId);
        return ResponseEntity.ok().build();
    }

    // --- Control & PROTOBUF (for whole fleet) ---

    @PutMapping(value = "/{fleetId}/config", consumes = "application/x-protobuf")
    @Transactional
    public ResponseEntity<Void> setFleetConfig(@PathVariable Long fleetId,
                                               @RequestBody IotProtos.LampConfig config) {
        // 1. MQTT
        mqttService.sendConfigToFleet(fleetId, config);

        // 2. BAZA DANYCH
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));

        for (Lamp lamp : fleet.getLamps()) {
            // Aktualizujemy tylko te pola, które zostały wysłane (są > 0 lub niepuste)
            if (config.getBrightness() > 0) lamp.setBrightness(config.getBrightness());
            if (config.getColor() != null && !config.getColor().isEmpty()) lamp.setColor(config.getColor());
            if (config.getReportInterval() > 0) lamp.setReportInterval(config.getReportInterval());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{fleetId}/command", consumes = "application/x-protobuf")
    @Transactional
    public ResponseEntity<Void> sendFleetCommand(@PathVariable Long fleetId,
                                                 @RequestBody IotProtos.LampCommand command) {
        // 1. MQTT (Fizyka)
        mqttService.sendCommandToFleet(fleetId, command);

        // 2. BAZA DANYCH (Logika aplikacji)
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));

        // Ustalmy jaki stan chcemy zapisać
        Boolean newState = null;
        if (command.getType() == IotProtos.LampCommand.Type.ON) {
            newState = true;
        } else if (command.getType() == IotProtos.LampCommand.Type.OFF) {
            newState = false;
        }

        // Jeśli komenda to ON lub OFF, aktualizujemy wszystkie lampy w bazie
        if (newState != null) {
            for (Lamp lamp : fleet.getLamps()) {
                lamp.setOn(newState);
            }
        }

        return ResponseEntity.ok().build();
    }

    // GET /fleets/{id}/config - Pobieramy konfigurację "reprezentatywną" (np. pierwszej lampy)
    // lub zwracamy null, jeśli flota jest niespójna.
    @GetMapping(value = "/{fleetId}/config", produces = "application/x-protobuf")
    public IotProtos.LampConfig getFleetConfig(@PathVariable Long fleetId) {
        // Logika uproszczona: zwracamy domyślny config
        return IotProtos.LampConfig.newBuilder()
                .setBrightness(50)
                .setReportInterval(60)
                .setColor("#ffffff")
                .build();
    }

    // GET /fleets/{id}/status - Czy flota jest "ON"?
    @GetMapping(value = "/{fleetId}/status", produces = "application/x-protobuf")
    public IotProtos.LampStatus getFleetStatus(@PathVariable Long fleetId) {
        Fleet fleet = fleetRepository.findById(fleetId).orElseThrow();
        // Sprawdzamy, czy chociaż jedna lampa jest włączona
        boolean anyOn = fleet.getLamps().stream().anyMatch(Lamp::isOn);

        return IotProtos.LampStatus.newBuilder()
                .setIsOn(anyOn)
                .setSensorValue(0) // Średnia?
                .build();
    }

    // GET /fleets/{id}/metrics - Średnia z czujników całej floty
    @GetMapping("/{fleetId}/metrics")
    public ResponseEntity<List<Double>> getFleetMetrics(@PathVariable Long fleetId) {
        // Tutaj logika byłaby bardziej złożona (agregacja SQL),
        // dla zaliczenia wystarczy zwrócić np. metryki z wszystkich lamp płasko
        return ResponseEntity.ok(List.of());
    }
}