package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.service.LampService;
import org.qualv13.iotbackend.service.MqttService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LampController.class)
class LampControllerProtoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LampService lampService;

    @MockBean
    private MqttService mqttService;

    @MockBean
    private org.qualv13.iotbackend.repository.LampMetricRepository lampMetricRepository; // Dodane, bo kontroler tego wymaga

    @Test
    void shouldAcceptProtobufConfig() throws Exception {
        // given
        // Tworzymy poprawny obiekt LampConfig zgodnie z nowym Proto
        IotProtos.InternalLampConfig internal = IotProtos.InternalLampConfig.newBuilder()
                .setReportingIntervalSeconds(30)
                .build();

        IotProtos.LampConfig config = IotProtos.LampConfig.newBuilder()
                .setVersion(1)
                .setInternalLampConfig(internal)
                .build();

        // when & then
        mockMvc.perform(put("/lamps/lamp_01/config")
                        .contentType("application/x-protobuf")
                        .content(config.toByteArray()))
                .andExpect(status().isOk());
    }
}