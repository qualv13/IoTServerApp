package org.qualv13.iotbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.backend.proto.IotProtos;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampAlert;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.config.LampModeConfig;
import org.qualv13.iotbackend.repository.LampAlertRepository;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LampService {
    private final LampRepository lampRepository;
    private final UserRepository userRepository;
    private final LampMetricRepository metricRepository;
    private final ObjectMapper objectMapper;
    private final LampAlertRepository alertRepository;

    @Lazy
    private final MqttService mqttService;

    // --- Logika przypisywania lampy ---
    @Transactional
    public String assignLampToUser(String username, String lampId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Lamp lamp = lampRepository.findById(lampId).orElse(
                new Lamp());


        if (lamp.getOwner() != null && !lamp.getOwner().getUsername().equals(username)) {
            log.info("Zmiana właściciela lampy {}. Czyszczenie danych historycznych.", lampId);
            // 1. Reset konfiguracji
            lamp.setModesConfigJson(null);
            lamp.setActiveModeId(null);

            lamp.setRed(0); lamp.setGreen(0); lamp.setBlue(0);
            lamp.setBrightness(50); lamp.setColor("#ffffff");
            lamp.setReportInterval(60);

            lamp.setColdWhite(0); lamp.setWarmWhite(0); lamp.setNeutralWhite(0);

            lamp.setLastAmbientLight(null); lamp.setLastAmbientNoise(null);

            lamp.setPhotoColorHue(0); lamp.setPhotoColorSaturation(0); lamp.setPhotoColorIntensity(0);

            lamp.setPhotoWhiteTemp(0); lamp.setPhotoWhiteIntensity(0);

            lamp.setModesConfigJson(null);

            lamp.setActiveModeId(null);

            // 2. Usuwanie METRYK (Historii temperatur, jasności itp.)
            metricRepository.deleteByLampId(lampId);

            // 3. Usuwanie ALERTÓW (Starych błędów)
            alertRepository.deleteByLampId(lampId);
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
        Lamp lamp = getLampWithAuthCheck(lampId);

        boolean stateChanged = false;

        // Ręczne ustawienie koloru
        if (command.hasSetDirectSettingsCommand()) {
            lamp.setOn(true);
            lamp.setOnline(true);
            IotProtos.DirectSettings ds = command.getSetDirectSettingsCommand().getDirectSettings();
            String hexColor = String.format("#%02x%02x%02x", ds.getRed(), ds.getGreen(), ds.getBlue());
            lamp.setColor(hexColor);

            // kanały kolorów
            lamp.setRed(ds.getRed());
            lamp.setGreen(ds.getGreen());
            lamp.setBlue(ds.getBlue());
            lamp.setWarmWhite(ds.getWarmWhite());
            lamp.setNeutralWhite(ds.getNeutralWhite());
            lamp.setColdWhite(ds.getColdWhite());

            lamp.setActiveModeId(null); // Wyjście z trybu automatycznego
            stateChanged = true;
        }
        // Ustawienie konkretnego trybu
        else if (command.hasSetModeCommand()) {
            lamp.setOn(true);
            lamp.setOnline(true);
            lamp.setActiveModeId(command.getSetModeCommand().getModeId());
            stateChanged = true;
        }
        // Blink LED, Reboot, OTA - nie zmieniają stanu w bazie trwale, tylko przelatują przez MQTT
        else if(command.hasSetPhotoWhiteSettingsCommand()){
            lamp.setOn(true);
            lamp.setOnline(true);
            lamp.setActiveModeId(null);
            IotProtos.PhotoWhiteSetting pws = command.getSetPhotoWhiteSettingsCommand().getPhotoWhiteSetting();
            lamp.setPhotoWhiteIntensity(pws.getIntensity());
            lamp.setPhotoWhiteTemp(pws.getTemperature());
            stateChanged = true;
        }
        else if (command.hasSetPhotoColorSettingsCommand()){
            lamp.setOn(true);
            lamp.setOnline(true);
            lamp.setActiveModeId(null);
            IotProtos.PhotoColorSetting pcs = command.getSetPhotoColorSettingsCommand().getPhotoColorSetting();
            lamp.setPhotoColorIntensity(pcs.getIntensity());
            lamp.setPhotoColorSaturation(pcs.getSaturation());
            lamp.setPhotoColorHue(pcs.getHue());
            stateChanged = true;
        }
        else if (command.hasSetWifiParamsCommand()) {
            log.info("Lampa {} aktualizuje WiFi SSID: {}", lampId, command.getSetWifiParamsCommand().getSsid());
        }

        if (stateChanged) {
            lampRepository.save(lamp);

            // WAŻNE: Tutaj wysyłamy komendę do fizycznej lampy!
            // Jeśli wywołanie przyszło z REST API, musimy popchnąć to na MQTT.
            // Sprawdzamy, czy to nie jest pętla zwrotna (opcjonalnie).
            // W prostym modelu: Zawsze wysyłamy.
            //mqttService.sendCommandToLamp(lampId, command);
        }
        //lampRepository.save(lamp);
    }

    @Transactional
    public void renameLamp(String lampId, String newName) {
        Lamp lamp = getLampWithAuthCheck(lampId);

        lamp.setDeviceName(newName);
        lampRepository.save(lamp);
    }

    // =========================================================
    //         GŁÓWNA LOGIKA KONFIGURACJI (PROTO <-> JSON)
    // =========================================================

    @Transactional
    public void updateLampConfig(String lampId, IotProtos.LampConfig protoConfig) {
        Lamp lamp = getLampWithAuthCheck(lampId);

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
                            javaEntry.setStartHour(entry.getHourSetting().getStartSecondPastMidnight()); // Konwersja sekundy -> godziny (uproszczenie)
                            javaEntry.setEndHour(entry.getHourSetting().getEndSecondPastMidnight());
                            javaEntry.setTransitionDurationSeconds(entry.getHourSetting().getTransitionDurationSeconds());
                        } else if (entry.hasBrightnessSettings()) {
                            javaEntry.setTriggerType("BRIGHTNESS");
                            javaEntry.setMinBrightness(entry.getBrightnessSettings().getMinBrightness());
                            javaEntry.setMaxBrightness(entry.getBrightnessSettings().getMaxBrightness());
                            int seconds = entry.getBrightnessSettings().getTransitionDurationSeconds();
                            javaEntry.setTransitionDurationSeconds(seconds);
                        }

                        // Action
                        if (entry.hasTargetState()) {
                            IotProtos.LampState state = entry.getTargetState();

                            if (state.hasDirectSettings()) {
                                javaEntry.setActionType("DIRECT");
                                var ds = state.getDirectSettings();
                                javaEntry.setRed(ds.getRed());
                                javaEntry.setGreen(ds.getGreen());
                                javaEntry.setBlue(ds.getBlue());
                                javaEntry.setWarmWhite(ds.getWarmWhite());
                                javaEntry.setColdWhite(ds.getColdWhite());
                                javaEntry.setNeutralWhite(ds.getNeutralWhite());
                                // Uwaga: w Twoim LampModeConfig może brakować pola neutralWhite,
                                // jeśli go nie ma, dodaj je lub zmapuj tutaj.
                            } else if (state.hasPhotoWhiteSetting()) {
                                javaEntry.setActionType("PHOTO_WHITE");
                                javaEntry.setIntensity(state.getPhotoWhiteSetting().getIntensity());
                                javaEntry.setTemperature(state.getPhotoWhiteSetting().getTemperature());
                            } else if (state.hasPhotoColorSetting()) {
                                javaEntry.setActionType("PHOTO_COLOR");
                                javaEntry.setIntensity(state.getPhotoColorSetting().getIntensity());
                                javaEntry.setHue(state.getPhotoColorSetting().getHue());
                                javaEntry.setSaturation(state.getPhotoColorSetting().getSaturation());
                            }
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
                            javaEntry.setNeutralWhite(ds.getNeutralWhite());
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
        saveConfigJsonToDb(lamp, protoConfig);
        lampRepository.save(lamp);
        //sendConfigToMqtt(lampId, protoConfig);
        //mqttService.sendConfigToLamp(lampId, protoConfig);
    }


    private void saveConfigJsonToDb(Lamp lamp, IotProtos.LampConfig protoConfig) {
        if (protoConfig.getModeSettingsCount() > 0) {
            List<LampModeConfig> modesList = new ArrayList<>();

            for (IotProtos.ModeCombinedSetting protoMode : protoConfig.getModeSettingsList()) {
                LampModeConfig javaMode = new LampModeConfig();
                javaMode.setModeId(protoMode.getModeId());
                javaMode.setName("Mode " + protoMode.getModeId()); // Domyślna nazwa

                // --- A. DISCO ---
                if (protoMode.hasDiscoModeSettings()) {
                    javaMode.setType("DISCO");
                    var pDisco = protoMode.getDiscoModeSettings();
                    javaMode.setDisco(new LampModeConfig.DiscoConfig(
                            pDisco.getMode().name(),
                            pDisco.getSpeed(),
                            pDisco.getIntensity()
                    ));
                }
                // --- B. HARMONOGRAM (SCHEDULE / DAYLIGHT) ---
                else if (protoMode.hasDaylightModeSettings()) {
                    javaMode.setType("SCHEDULE");
                    var pDaylight = protoMode.getDaylightModeSettings();
                    List<LampModeConfig.ScheduleEntry> entries = new ArrayList<>();

                    for (var entry : pDaylight.getScheduleEntriesList()) {
                        LampModeConfig.ScheduleEntry javaEntry = new LampModeConfig.ScheduleEntry();

                        // 1. Mapowanie Triggera (Wyzwalacza)
                        if (entry.hasHourSetting()) {
                            javaEntry.setTriggerType("HOUR");
                            // Konwersja sekund (Proto) na godziny (Java Model - uproszczenie dla UI)
                            // Jeśli potrzebujesz dokładności co do sekundy, zmień typ w LampModeConfig na seconds
                            javaEntry.setStartHour(entry.getHourSetting().getStartSecondPastMidnight());
                            javaEntry.setEndHour(entry.getHourSetting().getEndSecondPastMidnight());
                            javaEntry.setTransitionDurationSeconds(entry.getHourSetting().getTransitionDurationSeconds());
                        } else if (entry.hasBrightnessSettings()) {
                            javaEntry.setTriggerType("BRIGHTNESS");
                            javaEntry.setMinBrightness(entry.getBrightnessSettings().getMinBrightness());
                            javaEntry.setMaxBrightness(entry.getBrightnessSettings().getMaxBrightness());
                            javaEntry.setTransitionDurationSeconds(entry.getBrightnessSettings().getTransitionDurationSeconds());
                        }

                        // 2. Mapowanie Stanu Docelowego (Target State)
                        if (entry.hasTargetState()) {
                            IotProtos.LampState state = entry.getTargetState();

                            if (state.hasDirectSettings()) {
                                javaEntry.setActionType("DIRECT");
                                var ds = state.getDirectSettings();
                                javaEntry.setRed(ds.getRed());
                                javaEntry.setGreen(ds.getGreen());
                                javaEntry.setBlue(ds.getBlue());
                                javaEntry.setWarmWhite(ds.getWarmWhite());
                                javaEntry.setColdWhite(ds.getColdWhite());
                                javaEntry.setNeutralWhite(ds.getNeutralWhite()); // Odkomentuj jeśli dodasz to pole do LampModeConfig
                            } else if (state.hasPhotoWhiteSetting()) {
                                javaEntry.setActionType("PHOTO_WHITE");
                                var pw = state.getPhotoWhiteSetting();
                                javaEntry.setIntensity(pw.getIntensity());
                                javaEntry.setTemperature(pw.getTemperature());
                            } else if (state.hasPhotoColorSetting()) {
                                javaEntry.setActionType("PHOTO_COLOR");
                                var pc = state.getPhotoColorSetting();
                                javaEntry.setIntensity(pc.getIntensity());
                                javaEntry.setHue(pc.getHue());
                                javaEntry.setSaturation(pc.getSaturation());
                            }
                        }
                        entries.add(javaEntry);
                    }
                    javaMode.setSchedule(new LampModeConfig.ScheduleConfig(entries));
                }
                // --- C. PRESET ---
                else if (protoMode.hasPresetModeSetting()) {
                    javaMode.setType("PRESET");
                    var pPreset = protoMode.getPresetModeSetting();
                    List<LampModeConfig.PresetEntry> entries = new ArrayList<>();

                    // W nowym proto `presets` to lista obiektów `LampState`
                    for (IotProtos.LampState state : pPreset.getPresetsList()) {
                        LampModeConfig.PresetEntry javaEntry = new LampModeConfig.PresetEntry();

                        if (state.hasDirectSettings()) {
                            javaEntry.setType("DIRECT");
                            var ds = state.getDirectSettings();
                            javaEntry.setRed(ds.getRed());
                            javaEntry.setGreen(ds.getGreen());
                            javaEntry.setBlue(ds.getBlue());
                            javaEntry.setWarmWhite(ds.getWarmWhite());
                            javaEntry.setColdWhite(ds.getColdWhite());
                            javaEntry.setNeutralWhite(ds.getNeutralWhite());
                        } else if (state.hasPhotoWhiteSetting()) {
                            javaEntry.setType("WHITE");
                            var pw = state.getPhotoWhiteSetting();
                            javaEntry.setIntensity(pw.getIntensity());
                            javaEntry.setTemperature(pw.getTemperature());
                        } else if (state.hasPhotoColorSetting()) {
                            javaEntry.setType("COLOR");
                            var pc = state.getPhotoColorSetting();
                            javaEntry.setIntensity(pc.getIntensity());
                            javaEntry.setHue(pc.getHue());
                            javaEntry.setSaturation(pc.getSaturation());
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
                System.err.println("Błąd serializacji JSON configu: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @Transactional
    public void refreshLampConfig(String lampId) {
        // 1. Generujemy Proto na podstawie JSON-a z bazy
        IotProtos.LampConfig config = generateProtoFromDb(lampId);

        // 2. Wysyłamy do lampy
        mqttService.sendConfigToLamp(lampId, config);

        log.info("Wymuszono odświeżenie konfiguracji dla lampy {}", lampId);
    }

    // Metoda: DB JSON -> Java Object -> Proto
    private IotProtos.LampConfig generateProtoFromDb(String lampId) {
        Lamp lamp = getLampWithAuthCheck(lampId);

        IotProtos.LampConfig.Builder configBuilder = IotProtos.LampConfig.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000);

        // 1. Konfiguracja Wewnętrzna
        configBuilder.setInternalLampConfig(IotProtos.InternalLampConfig.newBuilder()
                .setReportingIntervalSeconds(lamp.getReportInterval() != null ? lamp.getReportInterval() : 60)
                .setWakeUpIntervalMinutes(10) // Domyślna wartość, lub dodaj pole do bazy
                .build());

        // 2. Konfiguracja Trybów (Modes)
        if (lamp.getModesConfigJson() != null && !lamp.getModesConfigJson().isEmpty()) {
            try {
                List<LampModeConfig> modesList = objectMapper.readValue(
                        lamp.getModesConfigJson(),
                        new TypeReference<List<LampModeConfig>>(){});

                for (LampModeConfig javaMode : modesList) {
                    IotProtos.ModeCombinedSetting.Builder modeBuilder = IotProtos.ModeCombinedSetting.newBuilder()
                            .setModeId(javaMode.getModeId());

                    // --- A. DISCO ---
                    if ("DISCO".equals(javaMode.getType()) && javaMode.getDisco() != null) {
                        try {
                            modeBuilder.setDiscoModeSettings(
                                    IotProtos.DiscoModeSettings.newBuilder()
                                            .setSpeed(javaMode.getDisco().getSpeed())
                                            .setIntensity(javaMode.getDisco().getIntensity())
                                            .setMode(IotProtos.DiscoModes.valueOf(javaMode.getDisco().getMode()))
                                            .build()
                            );
                        } catch (Exception e) {
                            // Fallback na OFF w razie błędu enuma
                            modeBuilder.setDiscoModeSettings(IotProtos.DiscoModeSettings.newBuilder().setMode(IotProtos.DiscoModes.OFF).build());
                        }
                    }
                    // --- B. HARMONOGRAM (SCHEDULE) ---
                    else if ("SCHEDULE".equals(javaMode.getType()) && javaMode.getSchedule() != null) {
                        IotProtos.DaylightModeSettings.Builder dayBuilder = IotProtos.DaylightModeSettings.newBuilder();

                        for (var ent : javaMode.getSchedule().getEntries()) {
                            IotProtos.ScheduleEntry.Builder entryBuilder = IotProtos.ScheduleEntry.newBuilder();

                            // 1. Trigger
                            if ("HOUR".equals(ent.getTriggerType())) {
                                entryBuilder.setHourSetting(IotProtos.HourSetting.newBuilder()
                                        .setStartSecondPastMidnight((ent.getStartHour() != null ? ent.getStartHour() : 0) * 3600)
                                        .setEndSecondPastMidnight((ent.getEndHour() != null ? ent.getEndHour() : 0) * 3600)
                                        .setEndsNextDay(ent.getEndHour() != null && ent.getStartHour() != null && ent.getEndHour() < ent.getStartHour())
                                        .setTransitionDurationSeconds((ent.getTransitionDurationSeconds() != null ? ent.getTransitionDurationSeconds() : 0))
                                        .build());
                            } else if ("BRIGHTNESS".equals(ent.getTriggerType())) {
                                entryBuilder.setBrightnessSettings(IotProtos.BrightnessSettings.newBuilder()
                                        .setMinBrightness(ent.getMinBrightness() != null ? ent.getMinBrightness() : 0)
                                        .setMaxBrightness(ent.getMaxBrightness() != null ? ent.getMaxBrightness() : 100)
                                        .setTransitionDurationSeconds(ent.getTransitionDurationSeconds() != null ? ent.getTransitionDurationSeconds() : 0)
                                        .build());
                            }

                            // 2. Target State (LampState)
                            IotProtos.LampState.Builder stateBuilder = IotProtos.LampState.newBuilder();

                            if ("DIRECT".equals(ent.getActionType())) {
                                stateBuilder.setDirectSettings(IotProtos.DirectSettings.newBuilder()
                                        .setRed(ent.getRed() != null ? ent.getRed() : 0)
                                        .setGreen(ent.getGreen() != null ? ent.getGreen() : 0)
                                        .setBlue(ent.getBlue() != null ? ent.getBlue() : 0)
                                        .setWarmWhite(ent.getWarmWhite() != null ? ent.getWarmWhite() : 0)
                                        .setColdWhite(ent.getColdWhite() != null ? ent.getColdWhite() : 0)
                                        .setNeutralWhite(ent.getNeutralWhite() != null ? ent.getNeutralWhite() : 0) // Default lub zmapuj z JSON
                                        .build());
                            } else if ("PHOTO_WHITE".equals(ent.getActionType())) {
                                stateBuilder.setPhotoWhiteSetting(IotProtos.PhotoWhiteSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setTemperature(ent.getTemperature() != null ? ent.getTemperature() : 0)
                                        .build());
                            } else if ("PHOTO_COLOR".equals(ent.getActionType())) {
                                stateBuilder.setPhotoColorSetting(IotProtos.PhotoColorSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setHue(ent.getHue() != null ? ent.getHue() : 0)
                                        .setSaturation(ent.getSaturation() != null ? ent.getSaturation() : 0)
                                        .build());
                            }

                            // Jeśli stan został poprawnie zbudowany, dodajemy go
                            if (stateBuilder.hasDirectSettings() || stateBuilder.hasPhotoWhiteSetting() || stateBuilder.hasPhotoColorSetting()) {
                                entryBuilder.setTargetState(stateBuilder.build());
                                dayBuilder.addScheduleEntries(entryBuilder);
                            }
                        }
                        modeBuilder.setDaylightModeSettings(dayBuilder);
                    }
                    // --- C. PRESET ---
                    else if ("PRESET".equals(javaMode.getType()) && javaMode.getPresets() != null) {
                        IotProtos.PresetModeSetting.Builder presetBuilder = IotProtos.PresetModeSetting.newBuilder();

                        for (var ent : javaMode.getPresets().getEntries()) {
                            IotProtos.LampState.Builder stateBuilder = IotProtos.LampState.newBuilder();

                            if ("DIRECT".equals(ent.getType())) {
                                stateBuilder.setDirectSettings(IotProtos.DirectSettings.newBuilder()
                                        .setRed(ent.getRed() != null ? ent.getRed() : 0)
                                        .setGreen(ent.getGreen() != null ? ent.getGreen() : 0)
                                        .setBlue(ent.getBlue() != null ? ent.getBlue() : 0)
                                        .setWarmWhite(ent.getWarmWhite() != null ? ent.getWarmWhite() : 0)
                                        .setColdWhite(ent.getColdWhite() != null ? ent.getColdWhite() : 0)
                                        .setNeutralWhite(ent.getNeutralWhite() != null ? ent.getNeutralWhite() : 0)
                                        .build());
                            } else if ("WHITE".equals(ent.getType())) {
                                stateBuilder.setPhotoWhiteSetting(IotProtos.PhotoWhiteSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setTemperature(ent.getTemperature() != null ? ent.getTemperature() : 0)
                                        .build());
                            } else if ("COLOR".equals(ent.getType())) {
                                stateBuilder.setPhotoColorSetting(IotProtos.PhotoColorSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setHue(ent.getHue() != null ? ent.getHue() : 0)
                                        .setSaturation(ent.getSaturation() != null ? ent.getSaturation() : 0)
                                        .build());
                            }

                            if (stateBuilder.hasDirectSettings() || stateBuilder.hasPhotoWhiteSetting() || stateBuilder.hasPhotoColorSetting()) {
                                presetBuilder.addPresets(stateBuilder);
                            }
                        }
                        modeBuilder.setPresetModeSetting(presetBuilder);
                    }

                    configBuilder.addModeSettings(modeBuilder);
                }
            } catch (Exception e) {
                System.err.println("Błąd deserializacji JSON configu przy odczycie: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return configBuilder.build();
    }

    public IotProtos.LampConfig getLampConfig(String lampId) {
        Lamp lamp = getLampWithAuthCheck(lampId);

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
                                        .setStartSecondPastMidnight((ent.getStartHour() != null ? ent.getStartHour() : 0) * 3600)
                                        .setEndSecondPastMidnight((ent.getEndHour() != null ? ent.getEndHour() : 0) * 3600)
                                        .setTransitionDurationSeconds((ent.getTransitionDurationSeconds() != null ? ent.getTransitionDurationSeconds() : 0))
                                        .build());
                            } else if ("BRIGHTNESS".equals(ent.getTriggerType())) {
                                entryBuilder.setBrightnessSettings(IotProtos.BrightnessSettings.newBuilder()
                                        .setMinBrightness(ent.getMinBrightness() != null ? ent.getMinBrightness() : 0)
                                        .setMaxBrightness(ent.getMaxBrightness() != null ? ent.getMaxBrightness() : 100)
                                                .setTransitionDurationSeconds((ent.getTransitionDurationSeconds() != null ? ent.getTransitionDurationSeconds() : 0))
                                        .build());
                            }

                            IotProtos.LampState.Builder stateBuilder = IotProtos.LampState.newBuilder();

                            // Action
                            if ("DIRECT".equals(ent.getActionType())) {
                                stateBuilder.setDirectSettings(IotProtos.DirectSettings.newBuilder()
                                        .setRed(ent.getRed() != null ? ent.getRed() : 0)
                                        .setGreen(ent.getGreen() != null ? ent.getGreen() : 0)
                                        .setBlue(ent.getBlue() != null ? ent.getBlue() : 0)
                                        .setWarmWhite(ent.getWarmWhite() != null ? ent.getWarmWhite() : 0)
                                        .setColdWhite(ent.getColdWhite() != null ? ent.getColdWhite() : 0)
                                        .setNeutralWhite(ent.getNeutralWhite() != null ? ent.getNeutralWhite() : 0) // Dodać jeśli jest w modelu Java
                                        .build());
                            } else if ("PHOTO_WHITE".equals(ent.getActionType())) {
                                stateBuilder.setPhotoWhiteSetting(IotProtos.PhotoWhiteSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setTemperature(ent.getTemperature() != null ? ent.getTemperature() : 0)
                                        .build());
                            } else if ("PHOTO_COLOR".equals(ent.getActionType())) {
                                stateBuilder.setPhotoColorSetting(IotProtos.PhotoColorSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setHue(ent.getHue() != null ? ent.getHue() : 0)
                                        .setSaturation(ent.getSaturation() != null ? ent.getSaturation() : 0)
                                        .build());
                            }

                            if (stateBuilder.hasDirectSettings() || stateBuilder.hasPhotoWhiteSetting() || stateBuilder.hasPhotoColorSetting()) {
                                entryBuilder.setTargetState(stateBuilder.build());
                                dayBuilder.addScheduleEntries(entryBuilder);
                            }
                        }
                        modeBuilder.setDaylightModeSettings(dayBuilder);
                    }
                    // PRESET
                    else if ("PRESET".equals(javaMode.getType()) && javaMode.getPresets() != null) {
                        IotProtos.PresetModeSetting.Builder presetBuilder = IotProtos.PresetModeSetting.newBuilder();

                        for (var ent : javaMode.getPresets().getEntries()) {
                            // Budujemy LampState, bo Presets to teraz lista LampState
                            IotProtos.LampState.Builder stateBuilder = IotProtos.LampState.newBuilder();

                            if ("DIRECT".equals(ent.getType())) {
                                stateBuilder.setDirectSettings(IotProtos.DirectSettings.newBuilder()
                                        .setRed(ent.getRed() != null ? ent.getRed() : 0)
                                        .setGreen(ent.getGreen() != null ? ent.getGreen() : 0)
                                        .setBlue(ent.getBlue() != null ? ent.getBlue() : 0)
                                        .setWarmWhite(ent.getWarmWhite() != null ? ent.getWarmWhite() : 0)
                                        .setColdWhite(ent.getColdWhite() != null ? ent.getColdWhite() : 0)
                                        .setNeutralWhite(ent.getNeutralWhite() != null ? ent.getNeutralWhite() : 0)
                                        .build());
                            } else if ("WHITE".equals(ent.getType())) {
                                stateBuilder.setPhotoWhiteSetting(IotProtos.PhotoWhiteSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setTemperature(ent.getTemperature() != null ? ent.getTemperature() : 0)
                                        .build());
                            } else if ("COLOR".equals(ent.getType())) {
                                stateBuilder.setPhotoColorSetting(IotProtos.PhotoColorSetting.newBuilder()
                                        .setIntensity(ent.getIntensity() != null ? ent.getIntensity() : 0)
                                        .setHue(ent.getHue() != null ? ent.getHue() : 0)
                                        .setSaturation(ent.getSaturation() != null ? ent.getSaturation() : 0)
                                        .build());
                            }

                            // Dodajemy do listy presetów
                            if (stateBuilder.hasDirectSettings() || stateBuilder.hasPhotoWhiteSetting() || stateBuilder.hasPhotoColorSetting()) {
                                presetBuilder.addPresets(stateBuilder);
                            }
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
    @Transactional(readOnly = true)
    public IotProtos.StatusReport getLampStatusReport(String lampId) {
        Optional<Lamp> lampOpt = Optional.of(getLampWithAuthCheck(lampId));
        if (lampOpt.isEmpty()) {
            // Jeśli nie ma lampy, nie ma sensu szukać metryk. Zwracamy pusty/null.
            return null;
        }
        Lamp lamp = lampOpt.get();
        return metricRepository.findTop100ByLampIdOrderByTimestampDesc(lampId).stream()
                .findFirst()
                .map(metric -> {
                    IotProtos.StatusReport.Builder reportBuilder = IotProtos.StatusReport.newBuilder()
                            .setVersion(1)
                            .setTs(metric.getDeviceTimestamp() != null ? metric.getDeviceTimestamp() : System.currentTimeMillis())
                            .setUptimeSeconds(metric.getUptimeSeconds() != null ? metric.getUptimeSeconds() : 0)
                            .setAmbientLight(metric.getAmbientLight() != null ? metric.getAmbientLight() : 0)
                            .setAmbientNoise(metric.getAmbientNoise() != null ? metric.getAmbientNoise() : 0)
                            .setFirmwareVersion(lamp.getFirmwareVersion() != null ? lamp.getFirmwareVersion() : "0");

                    if (metric.getTemperatures() != null && !metric.getTemperatures().isEmpty()) {
                        Arrays.stream(metric.getTemperatures().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(s -> {
                                    try { return (int) Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
                                })
                                .filter(Objects::nonNull)
                                .forEach(reportBuilder::addTemperatureReadings);
                    }

                    // --- 4. MAPOWANIE LED SETTINGS (Stan lampy) ---
                    IotProtos.DirectSettings.Builder directSettingsBuilder = IotProtos.DirectSettings.newBuilder();

                    directSettingsBuilder.setRed(lamp.getRed() != null ? lamp.getRed() : 0);
                    directSettingsBuilder.setGreen(lamp.getGreen() != null ? lamp.getGreen() : 0);
                    directSettingsBuilder.setBlue(lamp.getBlue() != null ? lamp.getBlue() : 0);
                    directSettingsBuilder.setColdWhite(lamp.getColdWhite() != null ? lamp.getColdWhite() : 0);
                    directSettingsBuilder.setNeutralWhite(lamp.getNeutralWhite() != null ? lamp.getNeutralWhite() : 0);
                    directSettingsBuilder.setWarmWhite(lamp.getWarmWhite() != null ? lamp.getWarmWhite() : 0);

                    reportBuilder.setLedSettings(directSettingsBuilder);

                    // --- MAPOWANIE ALERTÓW (Nowa część) ---

                    // 1. Pobieramy alerty z bazy danych
                    List<LampAlert> activeAlerts = alertRepository.findByLampIdAndIsActiveTrue(lampId);

                    for (LampAlert entity : activeAlerts) {
                        // Tworzymy builder alertu Protobuf
                        IotProtos.Alert.Builder protoAlert = IotProtos.Alert.newBuilder();

                        // Mapujemy ID i Wiadomość
                        protoAlert.setId(entity.getId().intValue());
                        protoAlert.setMessage(entity.getMessage() != null ? entity.getMessage() : "Unknown Error");

                        // Konwersja czasu (Java LocalDateTime -> Protobuf int64 timestamp)
                        if (entity.getTimestamp() != null) {
                            // Zakładamy UTC, zmień ZoneOffset jeśli używasz czasu lokalnego
                            protoAlert.setTs(entity.getTimestamp().toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
                        }

                        // Mapowanie CAUSE (Integer z bazy -> Enum Proto)
                        if (entity.getAlertCode() != null) {
                            IotProtos.AlertCauses cause = IotProtos.AlertCauses.forNumber(entity.getAlertCode());
                            protoAlert.setCause(cause != null ? cause : IotProtos.AlertCauses.GENERAL);
                        } else {
                            protoAlert.setCause(IotProtos.AlertCauses.GENERAL);
                        }

                        // Mapowanie LEVEL (Integer z bazy -> Enum Proto)
                        if (entity.getAlertLevel() != null) {
                            IotProtos.AlertLevels level = IotProtos.AlertLevels.forNumber(entity.getAlertLevel());
                            protoAlert.setLevel(level != null ? level : IotProtos.AlertLevels.ERROR);
                        } else {
                            protoAlert.setLevel(IotProtos.AlertLevels.ERROR);
                        }

                        protoAlert.setWillPersist(true);

                        // Dodajemy gotowy alert do raportu
                        reportBuilder.addActiveAlerts(protoAlert);
                    }
                    // --------------------------------------

                    return reportBuilder.build();
                })
                .orElse(IotProtos.StatusReport.newBuilder().setVersion(1).build());
    }

    private Lamp getLampWithAuthCheck(String lampId) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lampa nie istnieje: " + lampId));

        // Pobieramy nazwę zalogowanego użytkownika
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // --- UPROSZCZONA LOGIKA ADMINA (Szybki Fix) ---
        // Zamiast sprawdzać role w bazie, sprawdzamy "na sztywno" nazwę użytkownika.
        // Używamy equalsIgnoreCase, żeby zadziałało dla "admin", "Admin", "ADMIN".
        boolean isAdmin = "admin".equalsIgnoreCase(currentUsername);

        // Log dla Twojej pewności (zobaczysz to w konsoli)
        log.info("AuthCheck: User='{}', IsAdmin={}, Owner='{}'",
                currentUsername, isAdmin, lamp.getOwner() != null ? lamp.getOwner().getUsername() : "BRAK");

        // --- WARUNEK BEZPIECZEŃSTWA ---
        // Dostęp ma właściciel LUB admin.
        if (lamp.getOwner() != null && !lamp.getOwner().getUsername().equals(currentUsername) && !isAdmin) {
            log.warn("⛔ Odmowa dostępu dla '{}' do lampy '{}'", currentUsername, lampId);
            throw new org.springframework.security.access.AccessDeniedException("Nie masz uprawnień do sterowania tą lampą!");
        }

        return lamp;
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