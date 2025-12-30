package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class LampService {
    private final LampRepository lampRepository;
    private final UserRepository userRepository;

//    @Transactional
//    public void assignLampToUser(String username, String lampId) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        //Lamp lamp = lampRepository.findById(lampId).orElse(new Lamp());
//
//        // LOGIC:
//        // If lamp had owner, overwrite him (re-selling).
//        // add logs of event, ex. "Ownership transfer from X to Y"
//
//        // ZMIANA: Zamiast orElse(new Lamp()), rzucamy wyjątek
//        Lamp lamp = lampRepository.findById(lampId)
//                .orElseThrow(() -> new RuntimeException("Nie znaleziono lampy o ID: " + lampId + ". Skontaktuj się z administratorem."));
//
//        lamp.setId(lampId);
//        lamp.setOwner(user);
//        lamp.setFleet(null); // Fleet reset when assigning to another user
//
//        lampRepository.save(lamp);
//    }

    @Transactional
    public String assignLampToUser(String username, String lampId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Lamp lamp = lampRepository.findById(lampId)
                .orElse(new Lamp());

        // Reset danych przy zmianie właściciela
        if (lamp.getOwner() != null && !lamp.getOwner().getUsername().equals(username)) {
            // lampMetricRepository.deleteByLampId(lampId);
        }

        lamp.setId(lampId);
        lamp.setOwner(user);
        lamp.setFleet(null);

        // --- NOWA LOGIKA TOKENA ---

        // 1. Generujemy 32 losowe bajty
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        // 2. Zamieniamy na String HEX (to jest Twój TOKEN do wpisania w lampę)
        String rawToken = HexFormat.of().formatHex(randomBytes);

        // 3. Hashujemy SHA-256
        String tokenHash = calculateSha256(rawToken);

        // 4. Zapisujemy HASH w bazie
        lamp.setDeviceTokenHash(tokenHash);

        lampRepository.save(lamp);

        // 5. Zwracamy CZYSTY token użytkownikowi
        return rawToken;
    }

    @Transactional
    public void updateLampStatus(String lampId, IotProtos.LampCommand command) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        // Protobuf Enum -> Boolean
        // in .proto: enum Type { ON = 0; OFF = 1; }
        if (command.getType() == IotProtos.LampCommand.Type.ON) {
            lamp.setOn(true);
        } else if (command.getType() == IotProtos.LampCommand.Type.OFF) {
            lamp.setOn(false);
        }

        lampRepository.save(lamp);
    }

    @Transactional
    public void updateLampConfig(String lampId, IotProtos.LampConfig protoConfig) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        // Protobuf -> Entity
        lamp.setBrightness(protoConfig.getBrightness());
        lamp.setColor(protoConfig.getColor());
        lamp.setReportInterval(protoConfig.getReportInterval());

        lampRepository.save(lamp);
    }

    public IotProtos.LampConfig getLampConfig(String lampId) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        return IotProtos.LampConfig.newBuilder()
                .setBrightness(lamp.getBrightness() != null ? lamp.getBrightness() : 50)
                .setColor(lamp.getColor() != null ? lamp.getColor() : "#ffffff")
                .setReportInterval(lamp.getReportInterval() != null ? lamp.getReportInterval() : 60)
                .build();
    }

    public IotProtos.LampStatus getLampStatus(String lampId) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        return IotProtos.LampStatus.newBuilder()
                .setIsOn(lamp.isOn())
                .build();
    }

    private String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Błąd hashowania tokena", e);
        }
    }
}
