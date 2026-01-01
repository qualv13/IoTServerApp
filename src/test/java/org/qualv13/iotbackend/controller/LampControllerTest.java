package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.config.WebConfig;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.security.JwtService;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LampController.class)
// Importujemy WebConfig, żeby Spring wiedział jak obsługiwać "application/x-protobuf"
@Import(WebConfig.class)
@AutoConfigureMockMvc(addFilters = false) // Wyłączamy security dla uproszczenia testu kontrolera
class LampControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MqttService mqttService;
    @MockBean private LampService lampService;
    @MockBean private LampMetricRepository metricRepository;
    @MockBean private JwtService jwtService; // Potrzebne bo Security context wstaje

    @Test
    @WithMockUser
    void shouldSendCommandToLamp() throws Exception {
        // given
        String lampId = "lamp_test";
        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setBlinkLedCommand(IotProtos.BlinkLedCommand.newBuilder().setDuration(5).build())
                .build();

        // when & then
        mockMvc.perform(post("/lamps/" + lampId + "/command")
                        .contentType("application/x-protobuf") // Ważne!
                        .content(command.toByteArray()))
                .andExpect(status().isOk());

        // Weryfikacja czy serwis MQTT został wywołany
        verify(mqttService).sendCommandToLamp(eq(lampId), any(IotProtos.LampCommand.class));
    }
}