package org.qualv13.iotbackend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceStatusScheduler {

    private final LampRepository lampRepository;
    private final LampMetricRepository metricRepository;

    // Sprawdzaj co 20 sekund
    @Scheduled(fixedRate = 20000)
    public void checkOfflineLamps() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(120); // 2 minuty timeout

        // Pobieramy lampy, które są "Online" i sprawdzamy czy nie umarły
        lampRepository.findAll().stream()
                .filter(Lamp::isOnline)
                .forEach(lamp -> {
                    Optional<LampMetric> last = metricRepository.findFirstByLampIdOrderByTimestampDesc(lamp.getId());
                    if (last.isEmpty() || last.get().getTimestamp().isBefore(threshold)) {
                        lamp.setOnline(false);
                        lamp.setOn(false);
                        lampRepository.save(lamp);
                        log.warn("Lampa {} przeszła w stan OFFLINE (timeout)", lamp.getId());
                    }else if (last.get().getTimestamp().isAfter(threshold)) {
                        lamp.setOnline(true);
                        if(lamp.getBrightness() > 0) {
                            lamp.setOn(true);
                            log.info("Lampa {} świeci", lamp.getId());
                        }
                        lampRepository.save(lamp);
                    }
                    if(!lamp.isOnline()) {
                        lamp.setOn(false);
                        lampRepository.save(lamp);
                    }
                });


    }
}