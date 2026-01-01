package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.config.RabbitConfig;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RabbitMqListener {

    private final LampMetricRepository metricRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receiveMessage(byte[] payload,
                               @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String topic) {
        try {
            // Topic format: lamps/{lampId}/{messageType}
            String[] parts = topic.split("/");
            if (parts.length < 3) return;

            String lampId = parts[1];
            String msgType = parts[2]; // np. "status"

            // Obsługa StatusReport (wcześniej metrics)
            if ("status".equals(msgType)) {

                // 1. Parsowanie nowego Proto
                IotProtos.StatusReport report = IotProtos.StatusReport.parseFrom(payload);

                // 2. Mapowanie na bazę danych
                LampMetric metric = new LampMetric();
                metric.setLampId(lampId);
                metric.setTimestamp(LocalDateTime.now());
                metric.setDeviceTimestamp(report.getTs());
                metric.setUptimeSeconds(report.getUptimeSeconds());

                // Konwersja listy temperatur na String "20,21,22"
                String tempStr = report.getTemperatureReadingsList().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                metric.setTemperatures(tempStr);

                metricRepository.save(metric);

                System.out.println("Zapisano status dla lampy: " + lampId + ", Uptime: " + report.getUptimeSeconds());
            }

        } catch (Exception e) {
            System.err.println("Błąd dekodowania Proto: " + e.getMessage());
            e.printStackTrace();
        }
    }
}