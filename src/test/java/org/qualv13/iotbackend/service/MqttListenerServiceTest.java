//package org.qualv13.iotbackend.service;
//
//import com.iot.backend.proto.IotProtos;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.qualv13.iotbackend.entity.Lamp;
//import org.qualv13.iotbackend.entity.LampMetric;
//import org.qualv13.iotbackend.repository.LampMetricRepository;
//import org.qualv13.iotbackend.repository.LampRepository;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.Mockito.*;
//
//class MqttListenerServiceTest {
//
//    private final LampRepository lampRepository = mock(LampRepository.class);
//    private final LampMetricRepository metricRepository = mock(LampMetricRepository.class);
//    private final MqttListenerService listenerService = new MqttListenerService(lampRepository, metricRepository);
//
//    @Test
//    void shouldHandleStatusMessageAndSaveMetrics() throws Exception {
//        // Given
//        String lampId = "lamp_123";
//        String topic = "lamps/" + lampId + "/status";
//
//        // Mock DB
//        Lamp lamp = new Lamp();
//        lamp.setId(lampId);
//        when(lampRepository.findById(lampId)).thenReturn(Optional.of(lamp));
//
//        // Create Protobuf payload
//        IotProtos.LampStatus status = IotProtos.LampStatus.newBuilder()
//                .setIsOn(true)
//                .setSensorValue(25.5)
//                .build();
//
//        Message<byte[]> message = MessageBuilder
//                .withPayload(status.toByteArray())
//                .setHeader("mqtt_receivedTopic", topic)
//                .build();
//
//        // When
//        listenerService.handleMessage(message);
//
//        // Then
//        // 1. Verify Lamp Status Updated
//        assertTrue(lamp.isOn());
//        verify(lampRepository).save(lamp);
//
//        // 2. Verify Metric Saved
//        ArgumentCaptor<LampMetric> metricCaptor = ArgumentCaptor.forClass(LampMetric.class);
//        verify(metricRepository).save(metricCaptor.capture());
//
//        assertEquals(25.5, metricCaptor.getValue().getValue());
//        assertEquals(lamp, metricCaptor.getValue().getLamp());
//    }
//}