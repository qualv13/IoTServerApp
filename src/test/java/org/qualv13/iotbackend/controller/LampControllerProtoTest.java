package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos; // Twoja wygenerowana klasa
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qualv13.iotbackend.BaseIntegrationTest;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.qualv13.iotbackend.TestContainersConfig;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestContainersConfig.class) // <--- Dodaj to
class LampControllerProtoTest{

    @Autowired private MockMvc mockMvc;
    @Autowired private LampRepository lampRepository; // Dodaj autowired repo

    @MockBean private MqttService mqttService;

    @MockBean private MqttPahoClientFactory mqttClientFactory; // <--- Dodaj to
    @MockBean(name = "mqttInbound") private MqttPahoMessageDrivenChannelAdapter mqttInbound;

    @Test
    @WithMockUser(username = "admin") // Mock logged user
    void shouldAcceptProtobufConfig() throws Exception {
        // Given
        String lampId = "lamp_123";

        org.qualv13.iotbackend.entity.Lamp lamp = new org.qualv13.iotbackend.entity.Lamp();
        lamp.setId(lampId);
        lampRepository.save(lamp);

        IotProtos.LampConfig config = IotProtos.LampConfig.newBuilder()
                .setBrightness(100)
                .setColor("#FF0000")
                .setReportInterval(60)
                .build();

        // When & Then
        mockMvc.perform(put("/lamps/" + lampId + "/config")
                        .contentType("application/x-protobuf") // Content-Type
                        .content(config.toByteArray()))        // Send raw bytes
                .andExpect(status().isOk());

        // Verification if MQTT received objects
        Mockito.verify(mqttService).sendConfigToLamp(eq(lampId), any(IotProtos.LampConfig.class));
    }

    // Helper Mockito
    private IotProtos.LampConfig any(Class<IotProtos.LampConfig> clazz) {
        return Mockito.any(clazz);
    }
    private String eq(String str) {
        return Mockito.eq(str);
    }
}