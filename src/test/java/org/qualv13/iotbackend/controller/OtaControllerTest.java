//package org.qualv13.iotbackend.controller;
//
//import org.junit.jupiter.api.Test;
//import org.qualv13.iotbackend.service.MqttService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.hamcrest.Matchers.containsString;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class OtaControllerTest {
//
//    @Autowired private MockMvc mockMvc;
//
//    @MockBean private MqttService mqttService;
//    @MockBean(name = "mqttInbound") private MqttPahoMessageDrivenChannelAdapter mqttInbound;
//
//    @Test
//    @WithMockUser(username = "admin")
//    void shouldUploadFirmware() throws Exception {
//        // Given
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "firmware.bin",
//                "application/octet-stream",
//                new byte[]{1, 2, 3, 4}
//        );
//
//        // When & Then
//        mockMvc.perform(multipart("/ota/upload")
//                        .file(file)
//                        .param("lampId", "device_001"))
//                .andExpect(status().isOk())
//                .andExpect(content().string(containsString("OTA started")));
//
//        // (Opcjonalnie można zweryfikować czy plik powstał na dysku, ale mockowanie MqttService wystarczy do testu kontrolera)
//    }
//}