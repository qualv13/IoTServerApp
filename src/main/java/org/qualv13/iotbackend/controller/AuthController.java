package org.qualv13.iotbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.AuthResponse;
import org.qualv13.iotbackend.dto.LoginRequest;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.UserRepository;
import org.qualv13.iotbackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;import org.qualv13.iotbackend.dto.RefreshTokenRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("POST /auth/login");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Download user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")); // shouldn't happen after authentication

        // Token generation
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("POST /auth/refresh");
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                // Generating new tokens
                User user = (User) userDetails;

                String newAccessToken = jwtService.generateAccessToken(user);
                // optional : new refresh token
                String newRefreshToken = jwtService.generateRefreshToken(user);

                return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));
            }
        }
        return ResponseEntity.status(403).build();
    }
}