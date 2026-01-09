package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import org.qualv13.iotbackend.entity.LampMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lamps")
@RequiredArgsConstructor
public class LampController {

    private final MqttService mqttService;
    private final LampService lampService;
    private final LampMetricRepository metricRepository; // Przeniesione do konstruktora (Lombok)

    // --- STATUS (GET) ---
    @Operation(summary = "Pobierz status (Protobuf)", description = "Zwraca telemetrię urządzenia.")
    @ApiResponse(
            responseCode = "200",
            description = "Dane binarne Protobuf (StatusReport)",
            content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
    )
    @GetMapping(value = "/{lampId}/status", produces = "application/x-protobuf")
    public IotProtos.StatusReport getStatus(@PathVariable String lampId) {
        return lampService.getLampStatusReport(lampId);
    }

    // --- CONFIG (GET/PUT) ---
    @Operation(summary = "Pobierz konfigurację (Protobuf)")
    @ApiResponse(
            responseCode = "200",
            description = "Dane binarne Protobuf (LampConfig)",
            content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
    )
    @GetMapping(value = "/{lampId}/config", produces = "application/x-protobuf")
    public IotProtos.LampConfig getConfig(@PathVariable String lampId) {
        return lampService.getLampConfig(lampId);
    }

    @Operation(summary = "Zapisz konfigurację (Protobuf)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
            ))
    @PutMapping(value = "/{lampId}/config", consumes = "application/x-protobuf")
    public ResponseEntity<Void> setConfig(@PathVariable String lampId,
                                          @RequestBody IotProtos.LampConfig config) {
        lampService.updateLampConfig(lampId, config);
        mqttService.sendConfigToLamp(lampId, config);
        return ResponseEntity.ok().build();
    }

    // --- COMMAND (POST) ---
    @Operation(summary = "Wyślij komendę (Protobuf)", description = "Wysyła natychmiastowe polecenie do urządzenia.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
            ))    @ApiResponse(responseCode = "200", description = "Komenda wysłana")
    @PostMapping(value = "/{lampId}/command", consumes = "application/x-protobuf")
    public ResponseEntity<Void> sendCommand(@PathVariable String lampId,
                                            @RequestBody IotProtos.LampCommand command) {
        lampService.updateLampStateFromCommand(lampId, command);
        mqttService.sendCommandToLamp(lampId, command);
        return ResponseEntity.ok().build();
    }

    // --- METRICS (GET) ---
    @Operation(summary = "Pobierz historię temperatur (JSON)", description = "Zwraca listę wartości jako JSON.")
    @GetMapping("/{lampId}/metrics")
    public ResponseEntity<List<Double>> getMetrics(@PathVariable String lampId) {
        // ZMIANA: Parsujemy string "temp1,temp2" na double (bierzemy pierwszy)
        List<Double> values = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId)
                .stream()
                .map(metric -> {
                    String temps = metric.getTemperatures();
                    if (temps == null || temps.isEmpty()) return 0.0;
                    try {
                        // Bierzemy pierwszą temperaturę z listy
                        String firstTemp = temps.split(",")[0];
                        return Double.parseDouble(firstTemp);
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .toList();
        return ResponseEntity.ok(values);
    }
}