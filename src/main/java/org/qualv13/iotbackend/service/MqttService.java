package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MqttService {

    // Adapter wyjściowy MQTT (zdefiniowany w MqttConfig)
    private final MqttPahoMessageHandler mqttOutbound;

    // --- METODA POMOCNICZA (Wysyłanie bajtów) ---
    private void sendBytes(String topic, byte[] payload) {
        mqttOutbound.handleMessage(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", 1) // QoS 1 - gwarancja dostarczenia
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