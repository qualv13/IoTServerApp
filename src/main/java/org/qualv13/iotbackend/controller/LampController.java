package org.qualv13.iotbackend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.backend.proto.IotProtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.DetailedLampDto;
import org.qualv13.iotbackend.dto.MetricPoint;
import org.qualv13.iotbackend.dto.SmartConfigDto;
import org.qualv13.iotbackend.dto.UpdateLampNameRequest;
import org.qualv13.iotbackend.dto.json.PresetEntryJson;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.qualv13.iotbackend.dto.PresetDto;
import org.qualv13.iotbackend.dto.json.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/lamps")
@RequiredArgsConstructor
public class LampController {

    private final MqttService mqttService;
    private final LampService lampService;
    private final LampMetricRepository metricRepository;
    private final LampRepository lampRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                return ResponseEntity.ok(IotProtos.StatusReport.getDefaultInstance());
            }
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("ERROR GET /status for {}: {}", lampId, e.getMessage());
            e.printStackTrace();
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

    @Operation(summary = "Wymuś synchronizację konfiguracji", description = "Pobiera config z bazy i wysyła ponownie do lampy przez MQTT")
    @PostMapping("/{lampId}/config/refresh")
    public ResponseEntity<Void> refreshConfig(@PathVariable String lampId) {
        lampService.refreshLampConfig(lampId);
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

    // --- POBIERANIE PRESETÓW ---
    @GetMapping("/{lampId}/presets")
    public ResponseEntity<List<PresetDto>> getPresets(@PathVariable String lampId) {
        Lamp lamp = lampRepository.findById(lampId).orElseThrow();
        String json = lamp.getModesConfigJson();

        List<PresetDto> result = new ArrayList<>();

        if (json != null && !json.isEmpty()) {
            try {
                List<ModeConfigJson> modes = objectMapper.readValue(json, new TypeReference<>() {});

                ModeConfigJson presetMode = modes.stream()
                        .filter(m -> m.getModeId() == 3)
                        .findFirst()
                        .orElse(null);

                if (presetMode != null && presetMode.getPresets() != null && presetMode.getPresets().getEntries() != null) {
                    List<PresetEntryJson> entries = presetMode.getPresets().getEntries();
                    for (int i = 0; i < entries.size(); i++) {
                        PresetEntryJson entry = entries.get(i);
                        result.add(mapEntryToDto(i, entry));
                    }
                }
            } catch (Exception e) {
                log.error("Błąd parsowania JSON presetów", e);
            }
        }

        if (result.isEmpty()) {
            return ResponseEntity.ok(generateDefaultPresets());
        }

        return ResponseEntity.ok(result);
    }

    // Helper do generowania pustych slotów
    private List<PresetDto> generateDefaultPresets() {
        List<PresetDto> list = new ArrayList<>();
        for(int i=0; i<5; i++) {
            list.add(new PresetDto(i, "Pusty Slot " + (i+1), "direct", Map.of("red",0,"green",0,"blue",0)));
        }
        return list;
    }

    // --- ZAPISYWANIE PRESETÓW ---
    @PostMapping("/{lampId}/presets")
    @Transactional
    public ResponseEntity<Void> savePresets(@PathVariable String lampId, @RequestBody List<PresetDto> dtos) {
        Lamp lamp = lampRepository.findById(lampId).orElseThrow();
        String json = lamp.getModesConfigJson();

        try {
            List<ModeConfigJson> modes;
            if (json == null || json.isEmpty()) {
                modes = new ArrayList<>();
            } else {
                modes = objectMapper.readValue(json, new TypeReference<>() {});
            }

            ModeConfigJson presetMode = modes.stream()
                    .filter(m -> m.getModeId() == 3)
                    .findFirst()
                    .orElseGet(() -> {
                        ModeConfigJson newMode = new ModeConfigJson();
                        newMode.setModeId(3);
                        newMode.setName("Mode 3");
                        newMode.setType("PRESET");
                        newMode.setPresets(new PresetContainer());
                        modes.add(newMode);
                        return newMode;
                    });

            if (presetMode.getPresets() == null) presetMode.setPresets(new PresetContainer());

            // 2. Konwersja DTO (frontend) -> JSON Entry (baza)
            List<PresetEntryJson> newEntries = new ArrayList<>();
            dtos.sort(Comparator.comparingInt(PresetDto::getSlotIndex));

            for (PresetDto dto : dtos) {
                newEntries.add(mapDtoToEntry(dto));
            }

            presetMode.getPresets().setEntries(newEntries);

            // 3. Zapisz z powrotem do bazy jako String
            lamp.setModesConfigJson(objectMapper.writeValueAsString(modes));
            lampRepository.save(lamp);

        } catch (Exception e) {
            log.error("Błąd zapisu presetów", e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok().build();
    }

    // --- POMOCNICZE MAPERY ---

    private PresetDto mapEntryToDto(int index, PresetEntryJson entry) {
        Map<String, Object> data = new HashMap<>();
        String type = entry.getType().toLowerCase(); // "DIRECT" -> "direct"

        if ("direct".equalsIgnoreCase(type)) {
            data.put("red", entry.getRed());
            data.put("green", entry.getGreen());
            data.put("blue", entry.getBlue());
            data.put("warmWhite", entry.getWarmWhite());
            data.put("coldWhite", entry.getColdWhite());
            data.put("neutralWhite", entry.getNeutralWhite());
        } else if ("white".equalsIgnoreCase(type)) {
            data.put("intensity", entry.getIntensity());
            data.put("temperature", entry.getTemperature());
        } else if ("color".equalsIgnoreCase(type)) {
            data.put("hue", entry.getHue());
            data.put("saturation", entry.getSaturation());
            data.put("intensity", entry.getIntensity());
        }

        String name = "Slot " + (index + 1);
        return new PresetDto(index, name, type, data);
    }

    private PresetEntryJson mapDtoToEntry(PresetDto dto) {
        PresetEntryJson entry = new PresetEntryJson();
        String type = dto.getType().toUpperCase(); // "direct" -> "DIRECT"
        entry.setType(type);

        Map<String, Object> d = dto.getData(); // Skrót

        if ("DIRECT".equals(type)) {
            entry.setRed(toInt(d.get("red")));
            entry.setGreen(toInt(d.get("green")));
            entry.setBlue(toInt(d.get("blue")));
            entry.setWarmWhite(toInt(d.get("warmWhite")));
            entry.setColdWhite(toInt(d.get("coldWhite")));
            entry.setNeutralWhite(toInt(d.get("neutralWhite")));
        } else if ("WHITE".equals(type)) {
            entry.setIntensity(toInt(d.get("intensity")));
            entry.setTemperature(toInt(d.get("temperature")));
        } else if ("COLOR".equals(type)) {
            entry.setHue(toInt(d.get("hue")));
            entry.setSaturation(toInt(d.get("saturation")));
            entry.setIntensity(toInt(d.get("intensity")));
        }
        return entry;
    }

    private Integer toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return 0; // null safety
    }


    @Operation(summary = "Aktywuj preset (0-4)", description = "Włącza zdefiniowany wcześniej preset na lampie")
    @PostMapping("/{lampId}/presets/{index}")
    public ResponseEntity<Void> activatePreset(@PathVariable String lampId, @PathVariable int index) {
        log.info("POST /lamps/{}/presets/{}", lampId, index);
        mqttService.setPreset(lampId, index);
        return ResponseEntity.ok().build();
    }

    // --- Lamp Restart ---
    @Operation(summary = "Zrestartuj lampę")
    @PostMapping("/{lampId}/reboot")
    public ResponseEntity<Void> rebootLamp(@PathVariable String lampId) {
        log.info("POST /lamps/{}/reboot", lampId);

        IotProtos.RebootCommand reboot = IotProtos.RebootCommand.newBuilder().build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setRebootCommand(reboot)
                .build();

        mqttService.sendCommandToLamp(lampId, command);
        return ResponseEntity.ok().build();
    }

    // --- Lamp blink ---
    @Operation(summary = "Mrugnij lampą (Identify)")
    @PostMapping("/{lampId}/blink")
    public ResponseEntity<Void> blinkLamp(@PathVariable String lampId, @RequestParam(defaultValue = "5") int duration) {
        log.info("POST /lamps/{}/blink?duration={}", lampId, duration);

        IotProtos.BlinkLedCommand blink = IotProtos.BlinkLedCommand.newBuilder()
                .setDuration(duration)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setBlinkLedCommand(blink)
                .build();

        mqttService.sendCommandToLamp(lampId, command);
        return ResponseEntity.ok().build();
    }

    // --- WiFi change ---
    @Data
    public static class WifiConfigRequest {
        private String ssid;
        private String password;
    }

    @Operation(summary = "Zmień sieć WiFi lampy")
    @PostMapping("/{lampId}/wifi")
    public ResponseEntity<Void> setWifi(@PathVariable String lampId, @RequestBody WifiConfigRequest request) {
        log.info("POST /lamps/{}/wifi -> SSID: {}", lampId, request.getSsid());

        IotProtos.SetWifiParamsCommand wifi = IotProtos.SetWifiParamsCommand.newBuilder()
                .setSsid(request.getSsid())
                .setPassword(request.getPassword())
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setSetWifiParamsCommand(wifi)
                .build();

        mqttService.sendCommandToLamp(lampId, command);
        return ResponseEntity.ok().build();
    }

    // --- ALERTY ---
    @Data
    public static class AckAlertsRequest {
        private List<Integer> alertIds;
    }

    @Operation(summary = "Potwierdź alerty", description = "Wysyła do lampy informację, że użytkownik widział błędy")
    @PostMapping("/{lampId}/alerts/ack")
    public ResponseEntity<Void> acknowledgeAlerts(@PathVariable String lampId, @RequestBody AckAlertsRequest request) {
        log.info("POST /lamps/{}/alerts/ack -> {}", lampId, request.getAlertIds());
        mqttService.acknowledgeAlerts(lampId, request.getAlertIds());
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
                        String val = tempStr.split(",")[0].trim();
                        return Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .toList();

        return ResponseEntity.ok(values);
    }

    @GetMapping("/{lampId}/history")
    public ResponseEntity<List<MetricPoint>> getLampHistory(@PathVariable String lampId) {
        List<LampMetric> metrics = metricRepository.findTop50ByLampIdOrderByTimestampDesc(lampId);

        List<MetricPoint> history = metrics.stream()
                .map(m -> new MetricPoint(
                        m.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        m.getAmbientLight() != null ? m.getAmbientLight() : 0
                ))
                .sorted(Comparator.comparingLong(MetricPoint::getIdx))
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
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
            uptime = metric.getUptimeSeconds() != null
                    ? metric.getUptimeSeconds().longValue()
                    : 0L;

            if(metric.getTimestamp() != null) {
                lastUpdate = metric.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            if (metric.getAmbientLight() != null) ambLight = metric.getAmbientLight();
            if (metric.getAmbientNoise() != null) ambNoise = metric.getAmbientNoise();
        }

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