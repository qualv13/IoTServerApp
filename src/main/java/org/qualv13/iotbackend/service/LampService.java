package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LampService {
    private final LampRepository lampRepository;
    private final UserRepository userRepository;

    @Transactional
    public void assignLampToUser(String username, String lampId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Lamp lamp = lampRepository.findById(lampId).orElse(new Lamp());

        // LOGIC:
        // If lamp had owner, overwrite him (re-selling).
        // add logs of event, ex. "Ownership transfer from X to Y"

        lamp.setId(lampId);
        lamp.setOwner(user);
        lamp.setFleet(null); // Fleet reset when assigning to another user

        lampRepository.save(lamp);
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
}
