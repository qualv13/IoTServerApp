package org.qualv13.iotbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.qualv13.iotbackend.dto.AuthResponse;
import org.qualv13.iotbackend.dto.LoginRequest;
import org.qualv13.iotbackend.dto.RefreshTokenRequest;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.UserRepository;
import org.qualv13.iotbackend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldLoginSuccessfullyAndThenDeleteAccount() throws Exception {
        // Setup User
        User user = new User();
        user.setUsername("test_login3");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setLamps(new ArrayList<>());
        userRepository.save(user);

        LoginRequest req = new LoginRequest();
        req.setUsername("test_login3");
        req.setPassword("password123");

        // Perform Login
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        // Delete test user
        String responseJson = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        String accessToken = authResponse.getAccessToken();

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {
        // Setup User & Old Token
        User user = new User();
        user.setUsername("test_refresh2");
        user.setPassword(passwordEncoder.encode("pass"));
        userRepository.save(user);

        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(refreshToken);

        MvcResult refreshResult =  mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // Delete test user
        String responseJson = refreshResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        String accessToken = authResponse.getAccessToken();

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}