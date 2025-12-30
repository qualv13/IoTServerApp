package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MqttListenerService {

    private final LampRepository lampRepository;
    private final LampMetricRepository metricRepository;

    @ServiceActivator(inputChannel = "mqttInputChannel") // Musi pasować do nazwy beana w MqttConfig
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        Object payload = message.getPayload();

        // Topic format: lamps/{lampId}/status
        if (topic == null) return;
        String lampId = extractLampId(topic);

        try {
            // Obsługa STATUSU (np. włączenie/wyłączenie)
            if (topic.endsWith("/status")) {
                byte[] data = (payload instanceof byte[]) ? (byte[]) payload : payload.toString().getBytes();
                IotProtos.LampStatus status = IotProtos.LampStatus.parseFrom(data);

                // Logika aktualizacji statusu (ON/OFF)
                updateLampState(lampId, status);
            }
            // Obsługa METRYK (np. temperatura, wilgotność)
            else if (topic.endsWith("/metrics")) {
                // Zakładamy, że tutaj też przychodzi Protobuf LampStatus z wypełnionym sensor_value
                byte[] data = (payload instanceof byte[]) ? (byte[]) payload : payload.toString().getBytes();
                IotProtos.LampStatus metricData = IotProtos.LampStatus.parseFrom(data);

                saveMetric(lampId, metricData.getSensorValue());
            }
        } catch (Exception e) {
            System.err.println("Błąd przetwarzania MQTT [" + topic + "]: " + e.getMessage());
        }
    }

    // Wydzielona metoda do zapisu metryki
    private void saveMetric(String lampId, double value) {
        lampRepository.findById(lampId).ifPresent(lamp -> {
            LampMetric metric = new LampMetric(value, lamp);
            metricRepository.save(metric);
        });
    }

    // Wydzielona metoda do aktualizacji stanu
    private void updateLampState(String lampId, IotProtos.LampStatus status) {
        lampRepository.findById(lampId).ifPresent(lamp -> {
            lamp.setOn(status.getIsOn());
            lampRepository.save(lamp);
        });
    }

    private void saveStatusAndMetrics(String lampId, IotProtos.LampStatus status) {
        Optional<Lamp> lampOpt = lampRepository.findById(lampId);
        if (lampOpt.isPresent()) {
            Lamp lamp = lampOpt.get();

            // 1. Aktualizacja aktualnego stanu
            lamp.setOn(status.getIsOn());
            lampRepository.save(lamp);

            // 2. Zapis historii metryk
            if (status.getSensorValue() != 0) {
                LampMetric metric = new LampMetric(status.getSensorValue(), lamp);
                metricRepository.save(metric);
            }
        }
    }

    private String extractLampId(String topic) {
        // lamps/12345/status -> 12345
        String[] parts = topic.split("/");
        if (parts.length >= 2) return parts[1];
        return "";
    }
}