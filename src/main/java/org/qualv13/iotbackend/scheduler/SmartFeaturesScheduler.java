package org.qualv13.iotbackend.scheduler;

import com.iot.backend.proto.IotProtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.service.MqttService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmartFeaturesScheduler {

    private final LampRepository lampRepository;
    private final MqttService mqttService;

    // Uruchamiaj co 60 sekund
    @Scheduled(fixedRate = 60000)
    public void runSmartLogic() {
        List<Lamp> activeLamps = lampRepository.findAll().stream()
                .filter(Lamp::isOnline) // Tylko podłączone
                .filter(Lamp::isOn)     // Tylko włączone
                .toList();

        for (Lamp lamp : activeLamps) {
            boolean changed = false;

            if (lamp.isCircadianEnabled()) {
                applyCircadianRhythm(lamp);
                changed = true;
            }

            if (lamp.isAdaptiveBrightnessEnabled() && lamp.getLastAmbientLight() != null) {
                applyAdaptiveBrightness(lamp);
                changed = true;
            }

            if(changed) {
                lampRepository.save(lamp);
            }
        }
    }

    private void applyCircadianRhythm(Lamp lamp) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        int targetWarm = 0;
        int targetCold = 0;

        if (hour >= 6 && hour < 9) {
            targetWarm = 100; targetCold = 150;
        } else if (hour >= 9 && hour < 17) {
            targetWarm = 0; targetCold = 255;
        } else if (hour >= 17 && hour < 20) {
            targetWarm = 150; targetCold = 100;
        } else {
            targetWarm = 255; targetCold = 0;
        }

        if(lamp.getWarmWhite() - targetWarm > 10 || lamp.getColdWhite() - targetCold > 10) {
            lamp.setWarmWhite(targetWarm);
            lamp.setColdWhite(targetCold);
            lampRepository.save(lamp);
            sendWhiteCommand(lamp, targetCold, targetWarm);
        }
    }

    private void applyAdaptiveBrightness(Lamp lamp) {
        int lux = lamp.getLastAmbientLight();
        int currentBrightness = lamp.getBrightness();
        int targetBrightness = currentBrightness;

        if (lux > 800) {
            targetBrightness = 10;
        } else if (lux > 400) {
            targetBrightness = 40;
        } else if (lux < 100) {
            targetBrightness = 100;
        } else {
            targetBrightness = 70;
        }

        if (Math.abs(targetBrightness - currentBrightness) > 5) {
            log.info("Smart: Dostosowanie jasności lampy {} (Lux: {} -> Brightness: {})",
                    lamp.getId(), lux, targetBrightness);

            lamp.setBrightness(targetBrightness);
        }
    }

    private void sendWhiteCommand(Lamp lamp, int cold, int warm) {
        IotProtos.DirectSettings ds = IotProtos.DirectSettings.newBuilder()
                .setRed(0)
                .setGreen(0)
                .setBlue(0)
                .setColdWhite(cold)
                .setWarmWhite(warm)
                .setNeutralWhite(0)
                .build();

        IotProtos.SetDirectSettingsCommand cmd = IotProtos.SetDirectSettingsCommand.newBuilder()
                .setDirectSettings(ds)
                .build();

        IotProtos.LampCommand lampCmd = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis()/1000)
                .setSetDirectSettingsCommand(cmd)
                .build();

        mqttService.sendCommandToLamp(lamp.getId(), lampCmd);
    }
}