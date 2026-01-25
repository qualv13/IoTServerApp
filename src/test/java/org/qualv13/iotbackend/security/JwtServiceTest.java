//package org.qualv13.iotbackend.security;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.qualv13.iotbackend.entity.User;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@Transactional
//class JwtServiceTest {
//
//    @Autowired
//    private JwtService jwtService;
//
//    @Test
//    void shouldGenerateAndValidateToken() {
//        // Given
//        User user = new User();
//        user.setUsername("testuser");
//
//        // When
//        String token = jwtService.generateAccessToken(user);
//        String username = jwtService.extractUsername(token);
//        boolean isValid = jwtService.isTokenValid(token, user);
//
//        // Then
//        Assertions.assertNotNull(token);
//        Assertions.assertEquals("testuser", username);
//        Assertions.assertTrue(isValid);
//    }
//
//    @Test
//    void shouldFailForDifferentUser() {
//        // Given
//        User user1 = new User(); user1.setUsername("user1");
//        User user2 = new User(); user2.setUsername("user2");
//        String token = jwtService.generateAccessToken(user1);
//
//        // When
//        boolean isValid = jwtService.isTokenValid(token, user2);
//
//        // Then
//        Assertions.assertFalse(isValid);
//    }
//}