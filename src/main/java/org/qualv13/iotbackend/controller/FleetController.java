package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.dto.FleetDto;
import org.qualv13.iotbackend.dto.LampDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.service.FleetService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
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
        var fleets = fleetService.getMyFleets(principal.getName()).stream()
                .map(f -> new FleetDto(f.getId(), f.getName()))
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
            // Aktualizacja interwału, jeśli jest w configu
            if (config.hasInternalLampConfig()) {
                int interval = config.getInternalLampConfig().getReportingIntervalSeconds();
                if (interval > 0) lamp.setReportInterval(interval);
            }
            // Tutaj można dodać więcej logiki wyciągania danych z configu
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{fleetId}/command", consumes = "application/x-protobuf")
    @Transactional
    public ResponseEntity<Void> sendFleetCommand(@PathVariable Long fleetId,
                                                 @RequestBody IotProtos.LampCommand command) {
        // 1. MQTT
        mqttService.sendCommandToFleet(fleetId, command);

        // 2. BAZA DANYCH
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));

        // Próbujemy odgadnąć stan na podstawie komendy
        Boolean newState = null;
        if (command.hasSetDirectSettingsCommand()) {
            newState = true; // Ustawienie koloru = włączenie
        }
        // Jeśli dodasz komendę TurnOff, obsłuż ją tutaj:
        // else if (command.hasTurnOffCommand()) newState = false;

        if (newState != null) {
            for (Lamp lamp : fleet.getLamps()) {
                lamp.setOn(newState);
            }
        }

        return ResponseEntity.ok().build();
    }

    // GET Config floty (uproszczony)
    @GetMapping(value = "/{fleetId}/config", produces = "application/x-protobuf")
    public IotProtos.LampConfig getFleetConfig(@PathVariable Long fleetId) {
        return IotProtos.LampConfig.newBuilder()
                .setVersion(1)
                .setInternalLampConfig(
                        IotProtos.InternalLampConfig.newBuilder().setReportingIntervalSeconds(60).build()
                )
                .build();
    }

    // ZMIANA: Zwracamy StatusReport (agregacja jest trudna, zwracamy pusty)
    @GetMapping(value = "/{fleetId}/status", produces = "application/x-protobuf")
    public IotProtos.StatusReport getFleetStatus(@PathVariable Long fleetId) {
        // Nowy StatusReport nie ma pola "isOn", więc zwracamy pusty obiekt
        // Frontend i tak patrzy na listę lamp, a nie na ten endpoint dla floty.
        return IotProtos.StatusReport.newBuilder().setVersion(1).build();
    }

    @GetMapping("/{fleetId}/metrics")
    public ResponseEntity<List<Double>> getFleetMetrics(@PathVariable Long fleetId) {
        return ResponseEntity.ok(List.of());
    }
}