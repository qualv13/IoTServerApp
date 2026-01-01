//package org.qualv13.iotbackend.service;
//
//import com.iot.backend.proto.IotProtos;
//import org.qualv13.iotbackend.entity.Lamp;
//import org.qualv13.iotbackend.entity.LampMetric;
//import org.qualv13.iotbackend.repository.LampMetricRepository;
//import org.qualv13.iotbackend.repository.LampRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.integration.annotation.ServiceActivator;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessagingException;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class MqttListenerService {
//
//    private final LampRepository lampRepository;
//    private final LampMetricRepository metricRepository;
//
//    @ServiceActivator(inputChannel = "mqttInputChannel")
//    public void handleMessage(Message<?> message) throws MessagingException {
//        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
//        Object payload = message.getPayload();
//        byte[] data = (payload instanceof byte[]) ? (byte[]) payload : payload.toString().getBytes();
//
//        if (topic == null) return;
//        String lampId = extractLampId(topic);
//
//        try {
//            // 1. Obsługa prostego statusu (np. zmiana ON/OFF)
//            if (topic.endsWith("/status")) {
//                IotProtos.LampStatus status = IotProtos.LampStatus.parseFrom(data);
//                updateLampState(lampId, status.getIsOn());
//            }
//            // 2. Obsługa pełnego raportu telemetrycznego (Hex: 08 01 10...)
//            else if (topic.endsWith("/metrics")) {
//                IotProtos.StatusReport report = IotProtos.StatusReport.parseFrom(data);
//
//                // Zapisujemy uptime i wersję firmware (do encji Lamp)
//                updateLampInfo(lampId, report.getVersion(), report.getUptimeSeconds());
//
//                // Zapisujemy metryki (Temperatury)
//                // Zakładamy, że interesuje nas średnia lub ostatnia wartość
//                if (report.getTemperatureReadingsCount() > 0) {
//                    // Oblicz średnią z odczytów w liście
//                    double avgTemp = report.getTemperatureReadingsList().stream()
//                            .mapToDouble(Double::valueOf)
//                            .average().orElse(0.0);
//
//                    saveMetric(lampId, avgTemp);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Błąd deserializacji Protobuf dla [" + topic + "]: " + e.getMessage());
//        }
//    }
//
//    private void updateLampState(String lampId, boolean isOn) {
//        lampRepository.findById(lampId).ifPresent(lamp -> {
//            lamp.setOn(isOn);
//            lampRepository.save(lamp);
//        });
//    }
//
//    private void updateLampInfo(String lampId, int version, long uptime) {
//        lampRepository.findById(lampId).ifPresent(lamp -> {
//            // lamp.setFirmwareVersion(String.valueOf(version)); // Wymaga pola w Lamp.java
//            // Możemy tu logować uptime
//            lampRepository.save(lamp);
//        });
//    }
//
//    private void saveMetric(String lampId, double value) {
//        lampRepository.findById(lampId).ifPresent(lamp -> {
//            LampMetric metric = new LampMetric(value, lamp);
//            metricRepository.save(metric);
//        });
//    }
//
//    private String extractLampId(String topic) {
//        // format: lamps/{id}/...
//        String[] parts = topic.split("/");
//        return (parts.length >= 2) ? parts[1] : "";
//    }
//}