package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService {

    private final MqttPahoMessageHandler mqttOutbound;

    private void sendBytes(String topic, byte[] payload) {
        mqttOutbound.handleMessage(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", 1)
                .build());
    }

    // ==========================================
    //               POJEDYNCZE LAMPY
    // ==========================================

    /**
     * Wysyła konfigurację (np. interwał raportowania) do pojedynczej lampy.
     * Temat: lamps/{lampId}/config
     */
    public void sendConfigToLamp(String lampId, IotProtos.LampConfig config) {
        sendBytes("lamps/" + lampId + "/config", config.toByteArray());
    }

    /**
     * Wysyła komendę (np. zmiana koloru, reboot) do pojedynczej lampy.
     * Temat: lamps/{lampId}/command
     */
    public void sendCommandToLamp(String lampId, IotProtos.LampCommand command) {
        sendBytes("lamps/" + lampId + "/command", command.toByteArray());
    }

    public void sendRegistrationToken(String lampId, String token) {
        IotProtos.RegisterLampCommand regCmd = IotProtos.RegisterLampCommand.newBuilder()
                .setToken(token)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setRegisterLampCommand(regCmd)
                .build();

        sendBytes("lamps/" + lampId + "/command", command.toByteArray());
        log.info("Wysłano token rejestracyjny do lampy: {}", lampId);
    }

    public void setPreset(String lampId, int presetIndex) {
        IotProtos.SetPresetCommand presetCmd = IotProtos.SetPresetCommand.newBuilder()
                .setPresetIndex(presetIndex)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setSetPresetCommand(presetCmd)
                .build();

        sendCommandToLamp(lampId, command);
        log.info("MqttService: Wysłano preset {} do lampy {}", presetIndex, lampId);
    }

    public void acknowledgeAlerts(String lampId, List<Integer> alertIds) {
        IotProtos.AcknowledgeAlert ackCmd = IotProtos.AcknowledgeAlert.newBuilder()
                .addAllAlertIds(alertIds)
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setAcknowledgeAlert(ackCmd)
                .build();

        sendCommandToLamp(lampId, command);
        log.info("MqttService: Potwierdzono alerty {} dla lampy {}", alertIds, lampId);
    }

//    // ==========================================
//    //                  FLOTY
//    // ==========================================
//
//    /**
//     * Wysyła konfigurację do całej floty.
//     * Lampy muszą subskrybować temat: fleets/{fleetId}/config
//     */
//    public void sendConfigToFleet(Long fleetId, IotProtos.LampConfig config) {
//        sendBytes("fleets/" + fleetId + "/config", config.toByteArray());
//    }
//
//    /**
//     * Wysyła komendę do całej floty.
//     * Lampy muszą subskrybować temat: fleets/{fleetId}/command
//     */
//    public void sendCommandToFleet(Long fleetId, IotProtos.LampCommand command) {
//        sendBytes("fleets/" + fleetId + "/command", command.toByteArray());
//    }
}