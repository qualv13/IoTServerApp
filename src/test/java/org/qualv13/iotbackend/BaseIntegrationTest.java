//package org.qualv13.iotbackend;
//
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
//import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//@Import(TestContainersConfig.class) // Importujemy bazę danych
//public abstract class BaseIntegrationTest {
//
//    // --- GLOBALNE MOCKI DLA WSZYSTKICH TESTÓW ---
//
//    // 1. Blokuje tworzenie klienta MQTT (zapobiega łączeniu się)
//    @MockBean
//    protected MqttPahoClientFactory mqttClientFactory;
//
//    // 2. Blokuje adapter nasłuchujący (zapobiega błędom przy zamykaniu)
//    @MockBean(name = "mqttInbound")
//    protected MqttPahoMessageDrivenChannelAdapter mqttInbound;
//}