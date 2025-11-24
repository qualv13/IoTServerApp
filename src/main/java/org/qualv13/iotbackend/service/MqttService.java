package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MqttService {
    private final MessageChannel mqttOutboundChannel;

    public void sendCommand(String lampId, IotProtos.LampCommand command) {
        String topic = "lamps/" + lampId + "/command";
        mqttOutboundChannel.send(MessageBuilder
                .withPayload(command.toByteArray())
                .setHeader("mqtt_topic", topic)
                .build());
    }
}
