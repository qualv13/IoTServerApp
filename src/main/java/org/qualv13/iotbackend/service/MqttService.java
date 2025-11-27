package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos; // Zakładam, że masz wygenerowane klasy
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MqttService {
    private final MessageChannel mqttOutboundChannel;

    // Single lamp - command
    public void sendCommandToLamp(String lampId, IotProtos.LampCommand command) {
        String topic = "lamps/" + lampId + "/command";
        sendProto(topic, command.toByteArray());
    }

    // Single lamp - config
    public void sendConfigToLamp(String lampId, IotProtos.LampConfig config) {
        String topic = "lamps/" + lampId + "/config";
        sendProto(topic, config.toByteArray());
    }

    // Fleet - command
    public void sendCommandToFleet(Long fleetId, IotProtos.LampCommand command) {
        String topic = "fleets/" + fleetId + "/command";
        sendProto(topic, command.toByteArray());
    }

    // Fleet - config
    public void sendConfigToFleet(Long fleetId, IotProtos.LampConfig config) {
        String topic = "fleets/" + fleetId + "/config";
        sendProto(topic, config.toByteArray());
    }

    private void sendProto(String topic, byte[] payload) {
        mqttOutboundChannel.send(MessageBuilder
                .withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .build());
    }
}

//@Service
//@RequiredArgsConstructor
//public class MqttService {
//    private final MessageChannel mqttOutboundChannel;
//
//    public void sendCommand(String lampId, IotProtos.LampCommand command) {
//        String topic = "lamps/" + lampId + "/command";
//        mqttOutboundChannel.send(MessageBuilder
//                .withPayload(command.toByteArray())
//                .setHeader("mqtt_topic", topic)
//                .build());
//    }
//}
