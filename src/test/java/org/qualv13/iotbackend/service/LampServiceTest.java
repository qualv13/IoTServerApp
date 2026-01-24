//package org.qualv13.iotbackend.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.iot.backend.proto.IotProtos;
//import org.qualv13.iotbackend.entity.Lamp;
//import org.qualv13.iotbackend.model.LampModeConfig;
//import org.qualv13.iotbackend.repository.LampMetricRepository;
//import org.qualv13.iotbackend.repository.LampRepository;
//import org.qualv13.iotbackend.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class LampServiceTest {
//
//    @Mock private LampRepository lampRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private LampMetricRepository metricRepository;
//
//    // Używamy prawdziwego mappera, żeby przetestować czy JSON się dobrze generuje
//    private ObjectMapper objectMapper = new ObjectMapper();
//
//    private LampService lampService;
//
//    @BeforeEach
//    void setUp() {
//        lampService = new LampService(lampRepository, userRepository, metricRepository, objectMapper);
//    }
//
//    @Test
//    void shouldMapProtobufConfigToJsonAndSave() throws Exception {
//        // given
//        String lampId = "lamp_01";
//        Lamp lamp = new Lamp();
//        lamp.setId(lampId);
//
//        when(lampRepository.findById(lampId)).thenReturn(Optional.of(lamp));
//
//        // Tworzymy skomplikowany config Protobuf (Disco Mode)
//        IotProtos.DiscoModeSettings disco = IotProtos.DiscoModeSettings.newBuilder()
//                .setMode(IotProtos.DiscoModes.STROBE)
//                .setSpeed(80)
//                .setIntensity(100)
//                .build();
//
//        IotProtos.ModeCombinedSetting mode = IotProtos.ModeCombinedSetting.newBuilder()
//                .setModeId(1)
//                .setDiscoModeSettings(disco)
//                .build();
//
//        IotProtos.LampConfig config = IotProtos.LampConfig.newBuilder()
//                .addModeSettings(mode)
//                .build();
//
//        // when
//        lampService.updateLampConfig(lampId, config);
//
//        // then
//        ArgumentCaptor<Lamp> lampCaptor = ArgumentCaptor.forClass(Lamp.class);
//        verify(lampRepository).save(lampCaptor.capture());
//
//        Lamp savedLamp = lampCaptor.getValue();
//        assertThat(savedLamp.getModesConfigJson()).isNotNull();
//
//        // Sprawdzamy czy JSON zawiera nasze dane
//        String json = savedLamp.getModesConfigJson();
//        assertThat(json).contains("\"type\":\"DISCO\"");
//        assertThat(json).contains("\"mode\":\"STROBE\"");
//        assertThat(json).contains("\"speed\":80");
//
//        System.out.println("Generated JSON: " + json);
//    }
//
//    @Test
//    void shouldMapJsonToProtobufConfig() throws Exception {
//        // given
//        String lampId = "lamp_01";
//        String jsonConfig = "[{\"modeId\":1,\"name\":\"Party\",\"type\":\"DISCO\",\"disco\":{\"mode\":\"COLOR_CYCLE\",\"speed\":50,\"intensity\":90}}]";
//
//        Lamp lamp = new Lamp();
//        lamp.setId(lampId);
//        lamp.setModesConfigJson(jsonConfig);
//
//        when(lampRepository.findById(lampId)).thenReturn(Optional.of(lamp));
//
//        // when
//        IotProtos.LampConfig result = lampService.getLampConfig(lampId);
//
//        // then
//        assertThat(result.getModeSettingsCount()).isEqualTo(1);
//        IotProtos.ModeCombinedSetting mode = result.getModeSettings(0);
//
//        assertThat(mode.getModeId()).isEqualTo(1);
//        assertThat(mode.hasDiscoModeSettings()).isTrue();
//        assertThat(mode.getDiscoModeSettings().getMode()).isEqualTo(IotProtos.DiscoModes.COLOR_CYCLE);
//        assertThat(mode.getDiscoModeSettings().getSpeed()).isEqualTo(50);
//    }
//}