package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.DetailedLampDto;
import org.qualv13.iotbackend.dto.SmartConfigDto;
import org.qualv13.iotbackend.dto.UpdateLampNameRequest;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/lamps")
@RequiredArgsConstructor
public class LampController {

    private final MqttService mqttService;
    private final LampService lampService;
    private final LampMetricRepository metricRepository;
    private final LampRepository lampRepository;

    // --- STATUS (GET) ---
    @Operation(summary = "Pobierz status (Protobuf)", description = "Zwraca telemetrię urządzenia.")
    @ApiResponse(
            responseCode = "200",
            description = "Dane binarne Protobuf (StatusReport)",
            content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
    )
    @GetMapping(value = "/{lampId}/status", produces = "application/x-protobuf")
    public ResponseEntity<IotProtos.StatusReport> getStatus(@PathVariable String lampId) {
        try {
            IotProtos.StatusReport report = lampService.getLampStatusReport(lampId);

            if (report == null) {
                // Zwracamy pusty raport (Default Instance)
                return ResponseEntity.ok(IotProtos.StatusReport.getDefaultInstance());
            }
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("ERROR GET /status for {}: {}", lampId, e.getMessage());
            e.printStackTrace(); // Zobaczysz w konsoli Javy dokładny powód
            // W razie awarii zwracamy pusty raport z aktualnym czasem, żeby front nie "umarł"
            return ResponseEntity.ok(IotProtos.StatusReport.newBuilder()
                    .setVersion(1)
                    .setTs(System.currentTimeMillis() / 1000)
                    .build());
        }
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
        log.info("GET lamps/{}/config", lampId);
        return lampService.getLampConfig(lampId);
    }

    @Operation(summary = "Zapisz konfigurację (Protobuf)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/x-protobuf", schema = @Schema(type = "string", format = "binary"))
            ))
    @PutMapping(value = "/{lampId}/config", consumes = "application/x-protobuf")
    public ResponseEntity<Void> setConfig(@PathVariable String lampId,
                                          @RequestBody IotProtos.LampConfig config) {
        log.info("PUT /lamps/" + lampId + "/config");
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
        log.info("POST /lamps/{}/command", lampId);
        lampService.updateLampStateFromCommand(lampId, command);
        mqttService.sendCommandToLamp(lampId, command);
        return ResponseEntity.ok().build();
    }

    // --- METRICS (GET) ---
    @Operation(summary = "Pobierz historię temperatur (JSON)", description = "Zwraca listę wartości jako JSON.")
    @GetMapping("/{lampId}/metrics")
    public ResponseEntity<List<Double>> getMetrics(@PathVariable String lampId) {
        log.info("GET /lamps/" + lampId + "/metrics");
        List<Double> values = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId)
                .stream()
                .map(metric -> {
                    String tempStr = metric.getTemperatures();

                    if (tempStr == null || tempStr.isBlank()) {
                        return 0.0;
                    }

                    try {
                        // Parsowanie pierwszej wartości (np. "45,46" -> 45.0)
                        String val = tempStr.split(",")[0].trim();
                        return Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .toList();

        return ResponseEntity.ok(values);
    }

    @PutMapping("/{lampId}/name")
    public ResponseEntity<Void> updateLampName(@PathVariable String lampId,
                                               @RequestBody UpdateLampNameRequest request) {
        log.info("PUT /lamps/{}/name -> {}", lampId, request.getName());
        lampService.renameLamp(lampId, request.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{lampId}/smart-config")
    public ResponseEntity<Void> updateSmartConfig(@PathVariable String lampId,
                                                  @RequestBody SmartConfigDto request) {
        Lamp lamp = lampRepository.findById(lampId).orElseThrow();

        if (request.getCircadian() != null) {
            lamp.setCircadianEnabled(request.getCircadian());
            log.info("Lampa {}: Circadian = {}", lampId, request.getCircadian());
        }

        if (request.getAdaptive() != null) {
            lamp.setAdaptiveBrightnessEnabled(request.getAdaptive());
            log.info("Lampa {}: Adaptive = {}", lampId, request.getAdaptive());
        }

        lampRepository.save(lamp);
        return ResponseEntity.ok().build();
    }

    // --- DETAILED INFO (GET) ---
    @Operation(summary = "Pobierz szczegółowe informacje o lampie", description = "Zwraca pełne informacje o lampie wraz z aktualnymi metrykami.")
    @GetMapping("/{lampId}/details")
    public ResponseEntity<DetailedLampDto> getLampDetails(@PathVariable String lampId) {
        log.info("GET /lamps/{}/details", lampId);
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        // ZMIANA: Próg 15 sekund (szybka reakcja na offline)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(120);

        Optional<LampMetric> lastMetric = metricRepository.findFirstByLampIdOrderByTimestampDesc(lampId);

        boolean isCalculatedOnline = lastMetric
                .map(m -> m.getTimestamp().isAfter(threshold))
                .orElse(false);

        Double currentTemp = null;
        Long uptime = null;
        String lastUpdate = null;
        Integer ambLight = 0;
        Integer ambNoise = 0;

        if (lastMetric.isPresent()) {
            LampMetric metric = lastMetric.get();
            try {
                String tempStr = metric.getTemperatures();
                if (tempStr != null && !tempStr.isEmpty()) {
                    String val = tempStr.split(",")[0].trim();
                    if(!val.isEmpty()) {
                        currentTemp = Double.parseDouble(val);
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing temperature for lamp {}", lampId);
            }
            // Konwersja Integer -> Long z zabezpieczeniem na null
            uptime = metric.getUptimeSeconds() != null
                    ? metric.getUptimeSeconds().longValue()
                    : 0L;

            if(metric.getTimestamp() != null) {
                lastUpdate = metric.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            if (metric.getAmbientLight() != null) ambLight = metric.getAmbientLight();
            if (metric.getAmbientNoise() != null) ambNoise = metric.getAmbientNoise();
        }

        // Jeśli lampa jest Offline, to na pewno nie świeci (logicznie)
        boolean effectiveIsOn = isCalculatedOnline && lamp.isOn();

        DetailedLampDto dto = DetailedLampDto.builder()
                .id(lamp.getId())
                .deviceName(lamp.getDeviceName() != null ? lamp.getDeviceName() : "Unknown")
                .firmwareVersion(lamp.getFirmwareVersion())
                .isOn(effectiveIsOn)
                .isOnline(isCalculatedOnline)
                .brightness(lamp.getBrightness())
                .color(lamp.getColor())
                .reportInterval(lamp.getReportInterval())
                .fleetId(lamp.getFleet() != null ? lamp.getFleet().getId() : null)
                .activeModeId(lamp.getActiveModeId())
                .currentTemperature(currentTemp)
                .uptimeSeconds(uptime)
                .lastUpdate(lastUpdate)
                .ambientLight(ambLight)
                .ambientNoise(ambNoise)
                .isCircadianEnabled(lamp.isCircadianEnabled())
                .isAdaptiveBrightnessEnabled(lamp.isAdaptiveBrightnessEnabled())
                .build();

        return ResponseEntity.ok(dto);
    }
}