//package org.qualv13.iotbackend.service;
//
//import com.iot.backend.proto.IotProtos;
//import org.qualv13.iotbackend.entity.LampMetric;
//import org.qualv13.iotbackend.repository.LampMetricRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class RabbitMqListenerTest {
//
//    @Mock
//    private LampMetricRepository metricRepository;
//
//    @InjectMocks
//    private RabbitMqListener listener;
//
//    @Test
//    void shouldSaveStatusReportToDatabase() {
//        // given
//        String topic = "lamps/lamp_123/status";
//
//        // Tworzymy wiadomość Proto
//        IotProtos.StatusReport report = IotProtos.StatusReport.newBuilder()
//                .setTs(1600000000L)
//                .setUptimeSeconds(3600)
//                .addTemperatureReadings(25)
//                .addTemperatureReadings(26)
//                //.setBrightnessLevel(75.5f)
//                //.setPowerConsumptionWatts(12.5f)
//                .build();
//
//        byte[] payload = report.toByteArray();
//
//        // when
//        listener.receiveMessage(payload, topic);
//
//        // then
//        ArgumentCaptor<LampMetric> captor = ArgumentCaptor.forClass(LampMetric.class);
//        verify(metricRepository).save(captor.capture());
//
//        LampMetric saved = captor.getValue();
//        assertThat(saved.getLampId()).isEqualTo("lamp_123");
//        assertThat(saved.getUptimeSeconds()).isEqualTo(3600);
//        assertThat(saved.getTemperatures()).isEqualTo("25,26"); // Sprawdzamy konwersję listy na String
//        //assertThat(saved.getBrightness()).isEqualTo(75.5);
//        //assertThat(saved.getPowerWatts()).isEqualTo(12.5);
//    }
//}