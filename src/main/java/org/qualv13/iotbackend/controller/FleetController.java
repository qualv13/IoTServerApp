package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.dto.FleetDto;
import org.qualv13.iotbackend.dto.LampDto;
import org.qualv13.iotbackend.service.FleetService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> setFleetConfig(@PathVariable Long fleetId,
                                               @RequestBody IotProtos.LampConfig config) {
        mqttService.sendConfigToFleet(fleetId, config);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{fleetId}/command", consumes = "application/x-protobuf")
    public ResponseEntity<Void> sendFleetCommand(@PathVariable Long fleetId,
                                                 @RequestBody IotProtos.LampCommand command) {
        mqttService.sendCommandToFleet(fleetId, command);
        return ResponseEntity.ok().build();
    }

    // GET Mocks
    // TODO: add aggregated state from database
    @GetMapping(value = "/{fleetId}/config", produces = "application/x-protobuf")
    public IotProtos.LampConfig getFleetConfig(@PathVariable Long fleetId) {
        return IotProtos.LampConfig.newBuilder().build(); // Default
    }
    // TODO:
    @GetMapping(value = "/{fleetId}/status", produces = "application/x-protobuf")
    public IotProtos.LampStatus getFleetStatus(@PathVariable Long fleetId) {
        return IotProtos.LampStatus.newBuilder().setIsOn(true).build(); // Mock
    }
}