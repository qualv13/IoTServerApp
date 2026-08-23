package org.qualv13.iotbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.qualv13.iotbackend.BaseIntegrationTest;
import org.qualv13.iotbackend.dto.FleetDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FleetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FleetRepository fleetRepository;
    @Autowired private LampRepository lampRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @Transactional
    @WithMockUser(username = "fleet_master")
    void shouldCreateFleetAndAssignLamp() throws Exception {
        // Given: User and Lamp exist
        User user = new User();
        user.setUsername("fleet_master");
        user.setLamps(new ArrayList<>());
        userRepository.save(user);

        Lamp lamp = new Lamp();
        lamp.setId("lamp_fleet_1");
        lamp.setOwner(user);
        lampRepository.save(lamp);

        // 1. Create Fleet
        FleetDto dto = new FleetDto(null, "Living Room");
        mockMvc.perform(post("/fleets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        // Verify Fleet created in DB
        Fleet fleet = fleetRepository.findAll().get(0);
        assertEquals("Living Room", fleet.getName());

        // 2. Assign Lamp to Fleet
        mockMvc.perform(post("/fleets/" + fleet.getId() + "/lamps/" + lamp.getId()))
                .andExpect(status().isOk());

        // Verify Assignment
        Lamp updatedLamp = lampRepository.findById("lamp_fleet_1").orElseThrow();
        assertNotNull(updatedLamp.getFleet());
        assertEquals(fleet.getId(), updatedLamp.getFleet().getId());
    }

    @Test
    @WithMockUser(username = "fleet_master")
    void shouldListMyFleets() throws Exception {
        // Given
        User user = new User();
        user.setUsername("fleet_master");
        userRepository.save(user);

        Fleet fleet = new Fleet();
        fleet.setName("Kitchen");
        fleet.setOwner(user);
        fleetRepository.save(fleet);

        // When & Then
        mockMvc.perform(get("/fleets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kitchen"));
    }
}