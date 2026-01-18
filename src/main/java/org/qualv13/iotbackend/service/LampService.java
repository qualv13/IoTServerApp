package org.qualv13.iotbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.config.LampModeConfig;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LampService {
    private final LampRepository lampRepository;
    private final UserRepository userRepository;
    private final LampMetricRepository metricRepository;
    private final ObjectMapper objectMapper;

    // --- Logika przypisywania lampy ---
    @Transactional
    public String assignLampToUser(String username, String lampId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Lamp lamp = lampRepository.findById(lampId).orElse(
                new Lamp());


        if (lamp.getOwner() != null && !lamp.getOwner().getUsername().equals(username)) {
            lamp.setModesConfigJson(null); // Reset configu przy zmianie właściciela
            lamp.setActiveModeId(null);
        }

        lamp.setId(lampId);
        lamp.setOwner(user);
        lamp.setFleet(null);

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);
        lamp.setDeviceTokenHash(calculateSha256(rawToken));

        lampRepository.save(lamp);
        return rawToken;
    }

    // --- Obsługa komend (zmiana stanu/trybu) ---
    @Transactional
    public void updateLampStateFromCommand(String lampId, IotProtos.LampCommand command) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        // Ręczne ustawienie koloru
        if (command.hasSetDirectSettingsCommand()) {
            lamp.setOn(true);
            IotProtos.DirectSettings ds = command.getSetDirectSettingsCommand().getDirectSettings();
            String hexColor = String.format("#%02x%02x%02x", ds.getRed(), ds.getGreen(), ds.getBlue());
            lamp.setColor(hexColor);
            lamp.setRed(ds.getRed());
            lamp.setGreen(ds.getGreen());
            lamp.setBlue(ds.getBlue());
            lamp.setWarmWhite(ds.getWarmWhite());
            lamp.setNeutralWhite(ds.getNeutralWhite());
            lamp.setColdWhite(ds.getColdWhite());
            lamp.setActiveModeId(null); // Wyjście z trybu automatycznego
        }
        // Ustawienie konkretnego trybu
        else if (command.hasSetModeCommand()) {
            lamp.setOn(true);
            lamp.setActiveModeId(command.getSetModeCommand().getModeId());
        }
        // Blink LED, Reboot, OTA - nie zmieniają stanu w bazie trwale, tylko przelatują przez MQTT
        else if(command.hasSetPhotoWhiteSettingsCommand()){
            lamp.setOn(true);
            lamp.setActiveModeId(null);
            IotProtos.PhotoWhiteSetting pws = command.getSetPhotoWhiteSettingsCommand().getPhotoWhiteSetting();
            lamp.setPhotoWhiteIntensity(pws.getIntensity());
            lamp.setPhotoWhiteTemp(pws.getTemperature());
        }
        else if (command.hasSetPhotoColorSettingsCommand()){
            lamp.setOn(true);
            lamp.setActiveModeId(null);
            IotProtos.PhotoColorSetting pcs = command.getSetPhotoColorSettingsCommand().getPhotoColorSetting();
            lamp.setPhotoColorIntensity(pcs.getIntensity());
            lamp.setPhotoColorSaturation(pcs.getSaturation());
            lamp.setPhotoColorHue(pcs.getHue());
        }
        lampRepository.save(lamp);
    }

    @Transactional
    public void renameLamp(String lampId, String newName) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        lamp.setDeviceName(newName);
        lampRepository.save(lamp);
    }

    // =========================================================
    //         GŁÓWNA LOGIKA KONFIGURACJI (PROTO <-> JSON)
    // =========================================================

    @Transactional
    public void updateLampConfig(String lampId, IotProtos.LampConfig protoConfig) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        // 1. Konfiguracja wewnętrzna (Interwał)
        if (protoConfig.hasInternalLampConfig()) {
            int interval = protoConfig.getInternalLampConfig().getReportingIntervalSeconds();
            if (interval > 0) lamp.setReportInterval(interval);
        }

        // 2. Mapowanie PROTO -> JSON (Zapis do bazy)
        if (protoConfig.getModeSettingsCount() > 0) {
            List<LampModeConfig> modesList = new ArrayList<>();

            for (IotProtos.ModeCombinedSetting protoMode : protoConfig.getModeSettingsList()) {
                LampModeConfig javaMode = new LampModeConfig();
                javaMode.setModeId(protoMode.getModeId());
                javaMode.setName("Mode " + protoMode.getModeId());

                // A. DISCO
                if (protoMode.hasDiscoModeSettings()) {
                    javaMode.setType("DISCO");
                    var pDisco = protoMode.getDiscoModeSettings();
                    javaMode.setDisco(new LampModeConfig.DiscoConfig(
                            pDisco.getMode().name(),
                            pDisco.getSpeed(),
                            pDisco.getIntensity()
                    ));
                }
                // B. HARMONOGRAM (Daylight)
                else if (protoMode.hasDaylightModeSettings()) {
                    javaMode.setType("SCHEDULE");
                    var pDaylight = protoMode.getDaylightModeSettings();
                    List<LampModeConfig.ScheduleEntry> entries = new ArrayList<>();

                    for (var entry : pDaylight.getScheduleEntriesList()) {
                        LampModeConfig.ScheduleEntry javaEntry = new LampModeConfig.ScheduleEntry();

                        // Trigger
                        if (entry.hasHourSetting()) {
                            javaEntry.setTriggerType("HOUR");
                            javaEntry.setStartHour(entry.getHourSetting().getStartHour());
                            javaEntry.setEndHour(entry.getHourSetting().getEndHour());
                            javaEntry.setTransitionDurationMinutes(entry.getHourSetting().getTransitionDurationMinutes());
                        } else if (entry.hasBrightnessSettings()) {
                            javaEntry.setTriggerType("BRIGHTNESS");
                            javaEntry.setMinBrightness(entry.getBrightnessSettings().getMinBrightness());
                            javaEntry.setMaxBrightness(entry.getBrightnessSettings().getMaxBrightness());
                        }

                        // Action
                        if (entry.hasDirectSettings()) {
                            javaEntry.setActionType("DIRECT");
                            var ds = entry.getDirectSettings();
                            javaEntry.setRed(ds.getRed());
                            javaEntry.setGreen(ds.getGreen());
                            javaEntry.setBlue(ds.getBlue());
                            javaEntry.setWarmWhite(ds.getWarmWhite());
                            javaEntry.setColdWhite(ds.getColdWhite()); // Neutral white w proto = cold w javaDTO
                        } else if (entry.hasDiscoModeSettings()) {
                            javaEntry.setActionType("DISCO");
                            var dm = entry.getDiscoModeSettings();
                            javaEntry.setDiscoMode(dm.getMode().name());
                            javaEntry.setSpeed(dm.getSpeed());
                            javaEntry.setIntensity(dm.getIntensity());
                        } else if (entry.hasPhotoWhiteSetting()) {
                            javaEntry.setActionType("PHOTO_WHITE");
                            javaEntry.setIntensity(entry.getPhotoWhiteSetting().getIntensity());
                            javaEntry.setTemperature(entry.getPhotoWhiteSetting().getTemperature());
                        } else if (entry.hasPhotoColorSetting()) {
                            javaEntry.setActionType("PHOTO_COLOR");
                            javaEntry.setIntensity(entry.getPhotoColorSetting().getIntensity());
                            javaEntry.setHue(entry.getPhotoColorSetting().getHue());
                            javaEntry.setSaturation(entry.getPhotoColorSetting().getSaturation());
                        }

                        entries.add(javaEntry);
                    }
                    javaMode.setSchedule(new LampModeConfig.ScheduleConfig(entries));
                }
                // C. PRESET
                else if (protoMode.hasPresetModeSetting()) {
                    javaMode.setType("PRESET");
                    var pPreset = protoMode.getPresetModeSetting();
                    List<LampModeConfig.PresetEntry> entries = new ArrayList<>();

                    for (var entry : pPreset.getPresetsList()) {
                        LampModeConfig.PresetEntry javaEntry = new LampModeConfig.PresetEntry();
                        if (entry.hasDirectSettings()) {
                            javaEntry.setType("DIRECT");
                            var ds = entry.getDirectSettings();
                            javaEntry.setRed(ds.getRed());
                            javaEntry.setGreen(ds.getGreen());
                            javaEntry.setBlue(ds.getBlue());
                            javaEntry.setWarmWhite(ds.getWarmWhite());
                            javaEntry.setColdWhite(ds.getColdWhite()); // lub neutral
                        } else if (entry.hasPhotoWhiteSetting()) {
                            javaEntry.setType("WHITE");
                            javaEntry.setIntensity(entry.getPhotoWhiteSetting().getIntensity());
                            javaEntry.setTemperature(entry.getPhotoWhiteSetting().getTemperature());
                        } else if (entry.hasPhotoColorSetting()) {
                            javaEntry.setType("COLOR");
                            javaEntry.setIntensity(entry.getPhotoColorSetting().getIntensity());
                            javaEntry.setHue(entry.getPhotoColorSetting().getHue());
                            javaEntry.setSaturation(entry.getPhotoColorSetting().getSaturation());
                        }
                        entries.add(javaEntry);
                    }
                    javaMode.setPresets(new LampModeConfig.PresetConfig(entries));
                }

                modesList.add(javaMode);
            }

            try {
                String json = objectMapper.writeValueAsString(modesList);
                lamp.setModesConfigJson(json);
            } catch (Exception e) {
                System.err.println("Błąd serializacji JSON: " + e.getMessage());
            }
        }

        lampRepository.save(lamp);
    }

    public IotProtos.LampConfig getLampConfig(String lampId) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        IotProtos.LampConfig.Builder configBuilder = IotProtos.LampConfig.newBuilder()
                .setVersion(1);

        // Internal
        configBuilder.setInternalLampConfig(IotProtos.InternalLampConfig.newBuilder()
                .setReportingIntervalSeconds(lamp.getReportInterval() != null ? lamp.getReportInterval() : 60)
                .build());

        // Modes z JSON -> Proto
        if (lamp.getModesConfigJson() != null && !lamp.getModesConfigJson().isEmpty()) {
            try {
                List<LampModeConfig> modesList = objectMapper.readValue(
                        lamp.getModesConfigJson(),
                        new TypeReference<List<LampModeConfig>>(){});

                for (LampModeConfig javaMode : modesList) {
                    IotProtos.ModeCombinedSetting.Builder modeBuilder = IotProtos.ModeCombinedSetting.newBuilder()
                            .setModeId(javaMode.getModeId());

                    // DISCO
                    if ("DISCO".equals(javaMode.getType()) && javaMode.getDisco() != null) {
                        try {
                            modeBuilder.setDiscoModeSettings(
                                    IotProtos.DiscoModeSettings.newBuilder()
                                            .setSpeed(javaMode.getDisco().getSpeed())
                                            .setIntensity(javaMode.getDisco().getIntensity())
                                            .setMode(IotProtos.DiscoModes.valueOf(javaMode.getDisco().getMode()))
                                            .build()
                            );
                        } catch (IllegalArgumentException e) {
                            modeBuilder.setDiscoModeSettings(IotProtos.DiscoModeSettings.newBuilder().setMode(IotProtos.DiscoModes.OFF).build());
                        }
                    }
                    // SCHEDULE
                    else if ("SCHEDULE".equals(javaMode.getType()) && javaMode.getSchedule() != null) {
                        IotProtos.DaylightModeSettings.Builder dayBuilder = IotProtos.DaylightModeSettings.newBuilder();

                        for (var ent : javaMode.getSchedule().getEntries()) {
                            IotProtos.ScheduleEntry.Builder entryBuilder = IotProtos.ScheduleEntry.newBuilder();

                            // Trigger
                            if ("HOUR".equals(ent.getTriggerType())) {
                                entryBuilder.setHourSetting(IotProtos.HourSetting.newBuilder()
                                        .setStartHour(ent.getStartHour() != null ? ent.getStartHour() : 0)
                                        .setEndHour(ent.getEndHour() != null ? ent.getEndHour() : 0)
                                        .setTransitionDurationMinutes(ent.getTransitionDurationMinutes() != null ? ent.getTransitionDurationMinutes() : 0)
                                        .build());
                            } else if ("BRIGHTNESS".equals(ent.getTriggerType())) {
                                entryBuilder.setBrightnessSettings(IotProtos.BrightnessSettings.newBuilder()
                                        .setMinBrightness(ent.getMinBrightness() != null ? ent.getMinBrightness() : 0)
                                        .setMaxBrightness(ent.getMaxBrightness() != null ? ent.getMaxBrightness() : 100)
                                        .build());
                            }

                            // Action
                            if ("DIRECT".equals(ent.getActionType())) {
                                entryBuilder.setDirectSettings(IotProtos.DirectSettings.newBuilder()
                                        .setRed(ent.getRed() != null ? ent.getRed() : 0)
                                        .setGreen(ent.getGreen() != null ? ent.getGreen() : 0)
                                        .setBlue(ent.getBlue() != null ? ent.getBlue() : 0)
                                        .setWarmWhite(ent.getWarmWhite() != null ? ent.getWarmWhite() : 0)
                                        .setNeutralWhite(ent.getColdWhite() != null ? ent.getColdWhite() : 0)
                                        .build());
                            } else if ("DISCO".equals(ent.getActionType())) {
                                entryBuilder.setDiscoModeSettings(IotProtos.DiscoModeSettings.newBuilder()
                                        .setMode(IotProtos.DiscoModes.valueOf(ent.getDiscoMode()))
                                        .setSpeed(ent.getSpeed() != null ? ent.getSpeed() : 0)
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .build());
                            } else if ("PHOTO_WHITE".equals(ent.getActionType())) {
                                entryBuilder.setPhotoWhiteSetting(IotProtos.PhotoWhiteSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setTemperature(ent.getTemperature() != null ? ent.getTemperature() : 0)
                                        .build());
                            } else if ("PHOTO_COLOR".equals(ent.getActionType())) {
                                entryBuilder.setPhotoColorSetting(IotProtos.PhotoColorSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setHue(ent.getHue() != null ? ent.getHue() : 0)
                                        .setSaturation(ent.getSaturation() != null ? ent.getSaturation() : 0)
                                        .build());
                            }

                            dayBuilder.addScheduleEntries(entryBuilder);
                        }
                        modeBuilder.setDaylightModeSettings(dayBuilder);
                    }
                    // PRESET
                    else if ("PRESET".equals(javaMode.getType()) && javaMode.getPresets() != null) {
                        IotProtos.PresetModeSetting.Builder presetBuilder = IotProtos.PresetModeSetting.newBuilder();
                        for (var ent : javaMode.getPresets().getEntries()) {
                            IotProtos.PresetEntry.Builder pBuilder = IotProtos.PresetEntry.newBuilder();

                            if ("DIRECT".equals(ent.getType())) {
                                pBuilder.setDirectSettings(IotProtos.DirectSettings.newBuilder()
                                        .setRed(ent.getRed() != null ? ent.getRed() : 0)
                                        .setGreen(ent.getGreen() != null ? ent.getGreen() : 0)
                                        .setBlue(ent.getBlue() != null ? ent.getBlue() : 0)
                                        .setWarmWhite(ent.getWarmWhite() != null ? ent.getWarmWhite() : 0)
                                        .setNeutralWhite(ent.getColdWhite() != null ? ent.getColdWhite() : 0)
                                        .build());
                            } else if ("WHITE".equals(ent.getType())) {
                                pBuilder.setPhotoWhiteSetting(IotProtos.PhotoWhiteSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setTemperature(ent.getTemperature() != null ? ent.getTemperature() : 0)
                                        .build());
                            } else if ("COLOR".equals(ent.getType())) {
                                pBuilder.setPhotoColorSetting(IotProtos.PhotoColorSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setHue(ent.getHue() != null ? ent.getHue() : 0)
                                        .setSaturation(ent.getSaturation() != null ? ent.getSaturation() : 0)
                                        .build());
                            }
                            presetBuilder.addPresets(pBuilder);
                        }
                        modeBuilder.setPresetModeSetting(presetBuilder);
                    }

                    configBuilder.addModeSettings(modeBuilder);
                }
            } catch (Exception e) {
                System.err.println("Błąd deserializacji JSON configu: " + e.getMessage());
            }
        }

        return configBuilder.build();
    }

    // --- Status Report (Dane Telemetryczne) ---
    public IotProtos.StatusReport getLampStatusReport(String lampId) {
        Optional<Lamp> lampOpt = lampRepository.findById(lampId);
        if (lampOpt.isEmpty()) {
            // Jeśli nie ma lampy, nie ma sensu szukać metryk. Zwracamy pusty/null.
            return null;
        }
        Lamp lamp = lampOpt.get();
        return metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId).stream()
                .findFirst()
                .map(metric -> {
                    List<Integer> temps = new ArrayList<>();
                    if (metric.getTemperatures() != null && !metric.getTemperatures().isEmpty()) {
                        for (String t : metric.getTemperatures().split(",")) {
                            try { temps.add((int) Double.parseDouble(t)); } catch (NumberFormatException ignored) {}
                        }
                    }
                    IotProtos.DirectSettings.Builder directSettingsBuilder = IotProtos.DirectSettings.newBuilder();

                    directSettingsBuilder.setRed(lamp.getRed() != null ? lamp.getRed() : 0);
                    directSettingsBuilder.setGreen(lamp.getGreen() != null ? lamp.getGreen() : 0);
                    directSettingsBuilder.setBlue(lamp.getBlue() != null ? lamp.getBlue() : 0);
                    directSettingsBuilder.setColdWhite(lamp.getColdWhite() != null ? lamp.getColdWhite() : 0);
                    directSettingsBuilder.setNeutralWhite(lamp.getNeutralWhite() != null ? lamp.getNeutralWhite() : 0);
                    directSettingsBuilder.setWarmWhite(lamp.getWarmWhite() != null ? lamp.getWarmWhite() : 0);


                    //float power = metric.getPowerWatts() != null ? metric.getPowerWatts().floatValue() : 0.0f;
                    //float brightness = metric.getBrightness() != null ? metric.getBrightness().floatValue() : 0.0f;

                    return IotProtos.StatusReport.newBuilder()
                            .setVersion(1)
                            .setUptimeSeconds(metric.getUptimeSeconds() != null ? metric.getUptimeSeconds() : 0)
                            .setTs(metric.getDeviceTimestamp() != null ? metric.getDeviceTimestamp() : 0)
                            .addAllTemperatureReadings(temps)
                            .setAmbientLight(metric.getAmbientLight() != null ? metric.getAmbientLight() : 0)
                            .setAmbientNoise(metric.getAmbientNoise() != null ? metric.getAmbientNoise() : 0)
                            .setLedSettings(directSettingsBuilder)
                            //.setPowerConsumptionWatts(power)
                            //.setBrightnessLevel(brightness)
                            .build();
                })
                .orElse(IotProtos.StatusReport.newBuilder().setVersion(1).build());
    }

    private String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }
}