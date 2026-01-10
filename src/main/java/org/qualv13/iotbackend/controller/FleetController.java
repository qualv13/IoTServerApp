package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.FleetDto;
import org.qualv13.iotbackend.dto.LampDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import org.qualv13.iotbackend.service.FleetService;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/fleets")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;
    private final MqttService mqttService;
    private final FleetRepository fleetRepository;
    private final UserRepository userRepository;
    private final LampRepository lampRepository;
    private final LampService lampService;
    private final LampMetricRepository lampMetricRepository;

    // --- Fleet management ---
    @Operation(summary = "Pobierz moje floty", description = "Zwraca listę grup (flot) utworzonych przez użytkownika.")
    @GetMapping
    public ResponseEntity<List<FleetDto>> listMyFleets(Principal principal) {
        log.info("GET /fleets");
        var fleets = fleetService.getMyFleets(principal.getName()).stream()
                .map(f -> new FleetDto(f.getId(), f.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(fleets);
    }

    @Operation(summary = "Utwórz nową flotę", description = "Tworzy pustą grupę o podanej nazwie.")
    @PostMapping
    public ResponseEntity<Void> createFleet(@RequestBody FleetDto dto, Principal principal) {
        log.info("POST /fleets");
        fleetService.createFleet(dto.getName(), principal.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Usuń flotę", description = "Usuwa grupę. Lampy należące do grupy NIE są usuwane, tylko odpinane (fleet_id = null).")
    @DeleteMapping("/{fleetId}")
    @Transactional
    public ResponseEntity<Void> deleteFleet(@PathVariable Long fleetId, Authentication auth) {
        log.info("DELETE /fleets/{}", fleetId);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Fleet fleet = fleetRepository.findById(fleetId).orElseThrow();

        if (!fleet.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Odepnij lampy
        if (fleet.getLamps() != null) {
            fleet.getLamps().forEach(lamp -> {
                lamp.setFleet(null);
                lampRepository.save(lamp);
            });
        }

        fleetRepository.delete(fleet);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Zwraca wszystkie id lamp we flocie")
    @GetMapping("/{fleetId}/lamps")
    public ResponseEntity<List<LampDto>> getLampsInFleet(@PathVariable Long fleetId) {
        log.info("GET /fleets/{}/lamps", fleetId);
        return ResponseEntity.ok(fleetService.getLampsInFleet(fleetId));
    }

    @Operation(summary = "Dodaj lampę do floty", description = "Przypisuje lampę do podanej grupy. Lampa musi należeć do tego samego użytkownika.")
    @PostMapping("/{fleetId}/lamps/{lampId}")
    public ResponseEntity<Void> addLampToFleet(@PathVariable Long fleetId, @PathVariable String lampId) {
        log.info("POST /fleets/{}/lamps/{}", fleetId, lampId);
        fleetService.addLampToFleet(fleetId, lampId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Usuń lampę z floty", description = "Odpina lampę od grupy (ustawia fleet_id = null).")
    @DeleteMapping("/{fleetId}/lamps/{lampId}")
    public ResponseEntity<Void> removeLampFromFleet(@PathVariable Long fleetId, @PathVariable String lampId) {
        log.info("DELETE /fleets/{}/lamps/{}", fleetId, lampId);
        fleetService.removeLampFromFleet(fleetId, lampId);
        return ResponseEntity.ok().build();
    }

    // --- Control & PROTOBUF (for whole fleet) ---
    @Operation(summary = "Wyślij config do floty (Protobuf)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
            ))
    @PutMapping(value = "/{fleetId}/config", consumes = "application/x-protobuf")
    @Transactional
    public ResponseEntity<Void> setFleetConfig(@PathVariable Long fleetId,
                                               @RequestBody IotProtos.LampConfig config) {
        log.info("PUT /fleets/{}/config", fleetId);
        // 1. MQTT
        //mqttService.sendConfigToFleet(fleetId, config);

        // 2. BAZA DANYCH
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));

        for (Lamp lamp : fleet.getLamps()) {
            // Aktualizacja interwału, jeśli jest w configu
            if (config.hasInternalLampConfig()) {
                int interval = config.getInternalLampConfig().getReportingIntervalSeconds();
                if (interval > 0) lamp.setReportInterval(interval);
                mqttService.sendConfigToLamp(lamp.getId(), config);
            }
            // Tutaj można dodać więcej logiki wyciągania danych z configu
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Wyślij komendę do floty (Protobuf)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
            ))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Komenda wysłana"),
            @ApiResponse(responseCode = "400", description = "Błąd dekodowania Protobuf")
    })
    @PostMapping(value = "/{fleetId}/command", consumes = "application/x-protobuf")
    public ResponseEntity<Void> sendFleetCommand(@PathVariable Long fleetId,
                                                 @RequestBody IotProtos.LampCommand command) {
        log.info("POST /fleets/{}/command", fleetId);
        // 1. MQTT
        //mqttService.sendCommandToFleet(fleetId, command);

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

        if (newState != null || true) {
            for (Lamp lamp : fleet.getLamps()) {
                //lamp.setOn(newState);
                mqttService.sendCommandToLamp(lamp.getId(), command);
                lampService.updateLampStateFromCommand(lamp.getId(), command);
            }
        }

        return ResponseEntity.ok().build();
    }

    // GET Config floty (uproszczony)
    @Operation(summary = "Pobierz config floty (Protobuf)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/{fleetId}/config", produces = "application/x-protobuf")
    public IotProtos.LampConfig getFleetConfig(@PathVariable Long fleetId) {
        log.info("GET /fleets/{}/config", fleetId);
        return IotProtos.LampConfig.newBuilder()
                .setVersion(1)
                .setInternalLampConfig(
                        IotProtos.InternalLampConfig.newBuilder().setReportingIntervalSeconds(60).build()
                )
                .build();
    }

    // ZMIANA: Zwracamy StatusReport (agregacja jest trudna, zwracamy pusty)
    @Operation(summary = "Pobierz status floty (Protobuf)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/{fleetId}/status", produces = "application/x-protobuf")
    public IotProtos.StatusReport getFleetStatus(@PathVariable Long fleetId) {
        log.info("GET /fleets/" + fleetId + "/status");
        // Nowy StatusReport nie ma pola "isOn", więc zwracamy pusty obiekt
        // Frontend i tak patrzy na listę lamp, a nie na ten endpoint dla floty.
        return IotProtos.StatusReport.newBuilder().setVersion(1).build();
    }

    @Operation(summary = "Pobierz najnowsze temperatury ze wszystkich lamp")
    @GetMapping("/{fleetId}/metrics")
    public ResponseEntity<List<Double>> getFleetMetrics(@PathVariable Long fleetId) {
        log.info("GET /fleets/" + fleetId + "/metrics");
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));

        List<String> lampIds = fleet.getLamps().stream()
                .map(Lamp::getId)
                .toList();

        // 3. Jeśli flota jest pusta, zwróć pustą listę (żeby nie robić query z pustym IN)
        if (lampIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Double> out = lampMetricRepository.findLatestTemperaturesForLampIds(lampIds);


        return ResponseEntity.ok(out);
    }
}