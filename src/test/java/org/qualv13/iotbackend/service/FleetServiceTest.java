//package org.qualv13.iotbackend.service;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.qualv13.iotbackend.BaseIntegrationTest;
//import org.qualv13.iotbackend.entity.Fleet;
//import org.qualv13.iotbackend.entity.Lamp;
//import org.qualv13.iotbackend.repository.FleetRepository;
//import org.qualv13.iotbackend.repository.LampRepository;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//@Transactional
//class FleetServiceTest{
//
//    @Mock
//    private FleetRepository fleetRepository;
//    @Mock
//    private LampRepository lampRepository;
//
//    @InjectMocks
//    private FleetService fleetService;
//
//    @Test
//    void shouldAddLampToFleet() {
//        // Given
//        Long fleetId = 1L;
//        String lampId = "lamp_001";
//
//        Fleet fleet = new Fleet(); fleet.setId(fleetId);
//        Lamp lamp = new Lamp(); lamp.setId(lampId);
//
//        when(fleetRepository.findById(fleetId)).thenReturn(Optional.of(fleet));
//        when(lampRepository.findById(lampId)).thenReturn(Optional.of(lamp));
//
//        // When
//        fleetService.addLampToFleet(fleetId, lampId);
//
//        // Then
//        assertEquals(fleet, lamp.getFleet());
//        verify(lampRepository).save(lamp);
//    }
//}