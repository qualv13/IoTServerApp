package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.config.RabbitConfig;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampAlert;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.repository.LampAlertRepository;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import lombok.RequiredArgsConstructor;
import org.qualv13.iotbackend.repository.LampRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqListener {

    private final LampMetricRepository metricRepository;
    private final LampRepository lampRepository;
    private final LampAlertRepository alertRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receiveMessage(byte[] payload,
                               @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String topic) {
        log.info("Odebrano wiadomość z RabbitMQ! Temat: {}, Rozmiar: {} bajtów", topic, payload.length);
        try {
            // Topic format: lamps/{lampId}/{messageType}
            String[] parts = topic.split("\\.");
            //log.info("Coś przyszło");
            if (parts.length < 2) {
                log.warn("Ignoruję temat {} - za krótki format", topic); // Dodaj logowanie odrzuceń
                return;
            }
            String lampId = parts[1];
            String msgType = parts[2]; // np. "status"

            lampRepository.findById(lampId).ifPresent(lamp -> {
                if (!lamp.isOnline()) {
                    lamp.setOnline(true);
                    lampRepository.save(lamp);
                    log.info("Lampa {} jest teraz ONLINE", lampId);
                }
            });

            if (!lampRepository.existsById(lampId)) {
                log.warn("Otrzymano dane od nieznanej lampy: {}. Ignoruję.", lampId);
                return;
            }

            // Obsługa StatusReport (wcześniej metrics)
            if ("status".equals(msgType)) {
                Lamp lampEntity = lampRepository.findById(lampId).orElse(null);
                if (lampEntity == null) { return; }

                if (!lampEntity.isOnline()) {
                    lampEntity.setOnline(true);
                }

                // 1. Parsowanie nowego Proto
                IotProtos.StatusReport report = IotProtos.StatusReport.parseFrom(payload);

                // Zapisujemy odczyty do encji (dla Schedulera)
                lampEntity.setLastAmbientLight(report.getAmbientLight());
                lampEntity.setLastAmbientNoise(report.getAmbientNoise());
                lampEntity.setOnline(true); // Przy okazji odświeżamy status

                if(report.hasLedSettings()){
                    lampEntity.setRed(report.getLedSettings().getRed());
                    lampEntity.setGreen(report.getLedSettings().getGreen());
                    lampEntity.setBlue(report.getLedSettings().getBlue());
                    lampEntity.setColdWhite(report.getLedSettings().getColdWhite());
                    lampEntity.setNeutralWhite(report.getLedSettings().getNeutralWhite());
                    lampEntity.setWarmWhite(report.getLedSettings().getWarmWhite());
                }


                lampRepository.save(lampEntity);

                // 2. Mapowanie na bazę danych
                LampMetric metric = new LampMetric();
                metric.setLampId(lampId);
                metric.setTimestamp(LocalDateTime.now());
                metric.setDeviceTimestamp(report.getTs());
                metric.setUptimeSeconds(report.getUptimeSeconds());
                metric.setAmbientLight(report.getAmbientLight());
                metric.setAmbientNoise(report.getAmbientNoise());

                // Konwersja listy temperatur na String "20,21,22"
                String tempStr = report.getTemperatureReadingsList().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                metric.setTemperatures(tempStr);

                metric.setIsAbnormal(report.getIsAbnormal());

                if (report.getIsAbnormal()) {
                    log.warn("Otrzymano ABNORMAL REPORT od lampy {}", lampId);
                }

                metricRepository.save(metric);

                List<LampAlert> currentAlerts = alertRepository.findByLampIdAndIsActiveTrue(lampId);
                if (!currentAlerts.isEmpty()) {
                    // Deaktywuj wszystkie i zapisz nowe, lub usuń
                    alertRepository.deleteAll(currentAlerts);
                }

                if (report.getActiveAlertsCount() > 0) {
                    for (IotProtos.Alert protoAlert : report.getActiveAlertsList()) {
                        LampAlert alert = new LampAlert();
                        alert.setLampId(lampId);
                        alert.setAlertCode(protoAlert.getCauseValue());
                        alert.setAlertLevel(protoAlert.getLevelValue());
                        alert.setMessage(protoAlert.getMessage());
                        alert.setAlertIdFromDevice(protoAlert.getId());
                        alert.setTimestamp(LocalDateTime.now());
                        alert.setActive(true);

                        alertRepository.save(alert);
                        log.warn("⚠️ ALARM z lampy {}: {} (Level {})", lampId, protoAlert.getMessage(), protoAlert.getLevel());
                    }
                }

                //System.out.println("Zapisano status dla lampy: " + lampId + ", Uptime: " + report.getUptimeSeconds());
                log.info("Zapisano status dla lampy: {}, Uptime: {}", lampId, report.getUptimeSeconds());
            } else if ("command".equals(msgType)) {
                IotProtos.LampCommand command = IotProtos.LampCommand.parseFrom(payload);

                log.info("Odebrano command: {}", command);
            } else{
                log.warn("Wiadomość przyszła, ale nie ma \"status\" :(  {}", msgType);
            }

        } catch (Exception e) {
            //System.err.println("Błąd dekodowania Proto: " + e.getMessage());
            //e.printStackTrace();
            log.error("Błąd dekodowania Proto dla tematu {}: {}", topic, e.getMessage(), e);
        }
    }
}