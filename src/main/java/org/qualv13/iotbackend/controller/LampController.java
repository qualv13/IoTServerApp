package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import org.qualv13.iotbackend.entity.LampMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lamps")
@RequiredArgsConstructor
public class LampController {

    private final MqttService mqttService;
    private final LampService lampService;

    // --- STATUS (GET) ---
    @GetMapping(value = "/{lampId}/status", produces = "application/x-protobuf")
    public IotProtos.LampStatus getStatus(@PathVariable String lampId) {
        // TODO: Download latest known status from database (saved by MQTT Listener)
        return lampService.getLampStatus(lampId);
    }

    // --- CONFIG (GET/PUT) ---
    @GetMapping(value = "/{lampId}/config", produces = "application/x-protobuf")
    public IotProtos.LampConfig getConfig(@PathVariable String lampId) {
        return lampService.getLampConfig(lampId);
    }

    @PutMapping(value = "/{lampId}/config", consumes = "application/x-protobuf")
    public ResponseEntity<Void> setConfig(@PathVariable String lampId,
                                          @RequestBody IotProtos.LampConfig config) {
        // Save in db
        lampService.updateLampConfig(lampId, config);
        // Send through MQTT
        mqttService.sendConfigToLamp(lampId, config);
        return ResponseEntity.ok().build();
    }

    // --- COMMAND (POST) ---
    @PostMapping(value = "/{lampId}/command", consumes = "application/x-protobuf")
    public ResponseEntity<Void> sendCommand(@PathVariable String lampId,
                                            @RequestBody IotProtos.LampCommand command) {
        // Save in db
        lampService.updateLampStatus(lampId, command);
        // Send through MQTT
        mqttService.sendCommandToLamp(lampId, command);
        return ResponseEntity.ok().build();
    }

//    // --- METRICS (GET) ---
//    @GetMapping("/{lampId}/metrics")
//    public ResponseEntity<String> getMetrics(@PathVariable String lampId) {
//
//        // TODO: Implement metrics from lamp get
//        return ResponseEntity.ok("{\"metrics\": []}");
//    }

    @Autowired
    private LampMetricRepository metricRepository;

    @GetMapping("/{lampId}/metrics")
    public ResponseEntity<List<Double>> getMetrics(@PathVariable String lampId) {
        // Zwracamy tylko wartości dla uproszczenia (lub stwórz DTO z czasem)
        List<Double> values = metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId)
                .stream().map(LampMetric::getValue).toList();
        return ResponseEntity.ok(values);
    }
}