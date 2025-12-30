package org.qualv13.iotbackend.controller;

import org.junit.jupiter.api.Test;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @Test
    @WithMockUser(username = "user_to_delete") // Mock user
    void shouldDeleteOwnAccount() throws Exception {
        // Given
        User user = new User();
        user.setUsername("user_to_delete");
        user.setPassword("pass");
        user.setLamps(new ArrayList<>());
        userRepository.save(user);

        // When
        mockMvc.perform(delete("/users/me"))
                .andExpect(status().isOk());

        // Then
        assertTrue(userRepository.findByUsername("user_to_delete").isEmpty());
    }

    @Test
    void shouldForbidDeleteWithoutAuth() throws Exception {
        mockMvc.perform(delete("/users/me"))
                .andExpect(status().isForbidden()); // 403
    }

    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper; // Dodaj pole

    @Test
    void shouldRegisterUser() throws Exception {
        org.qualv13.iotbackend.dto.RegisterDto dto = new org.qualv13.iotbackend.dto.RegisterDto();
        dto.setUsername("new_user_reg");
        dto.setPassword("safe_password");

        mockMvc.perform(post("/users")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        assertTrue(userRepository.findByUsername("new_user_reg").isPresent());
    }
}

//package org.qualv13.iotbackend.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.qualv13.iotbackend.dto.AddLampDto;
//import org.qualv13.iotbackend.dto.RegisterDto;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class UserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    // Test 1: Register (Endpoint public)
//    @Test
//    void shouldRegisterUser() throws Exception {
//        RegisterDto dto = new RegisterDto();
//        dto.setUsername("test_integration");
//        dto.setPassword("pass123");
//
//        mockMvc.perform(post("/users")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto)))
//                .andExpect(status().isOk());
//    }
//
//    // Test 2: Add lamp (Requires logged user @WithMockUser)
//    @Test
//    @WithMockUser(username = "admin")
//    void shouldAddLampWhenAuthorized() throws Exception {
//
//        AddLampDto dto = new AddLampDto("lamp_999");
//
//        mockMvc.perform(post("/users/me/lamps")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto))) // we expect logic error, not security
//                .andExpect(status().is(500));
//    }
//
//    // Test 3: Get in without logging (expected 403)
//    @Test
//    void shouldRejectAnonymousUser() throws Exception {
//        mockMvc.perform(get("/users/me/lamps"))
//                .andExpect(status().isForbidden()); // 403
//    }
//}