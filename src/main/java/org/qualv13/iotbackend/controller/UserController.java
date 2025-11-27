package org.qualv13.iotbackend.controller;

import org.qualv13.iotbackend.dto.AddLampDto;
import org.qualv13.iotbackend.dto.RegisterDto;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import org.qualv13.iotbackend.service.LampService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.qualv13.iotbackend.dto.LampDto;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.stream.Collectors;
import org.qualv13.iotbackend.dto.ChangePasswordRequest;
import org.qualv13.iotbackend.dto.UpdateUserDto;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LampService lampService;
    private final LampRepository lampRepository;

    @PostMapping
    public ResponseEntity<String> registerUser(@RequestBody RegisterDto dto) {
        // Check if user exists
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        // Create user
        User user = new User();
        user.setUsername(dto.getUsername());
        // Password must be stored as hash
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    // Username change (PUT /users/me)
    @PutMapping("/me")
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserDto dto, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        // Must be unique
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            user.setUsername(dto.getUsername());
        }
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    // Change password (PUT /users/me/password)
    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Password changed");
    }

    // Delete account (DELETE /users/me)
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        // Set lamp owner to null, don't delete them from database
        if(user.getLamps() != null) {
            user.getLamps().forEach(lamp -> {
                lamp.setOwner(null);
                lampRepository.save(lamp);
            });
        }

        userRepository.delete(user);
        return ResponseEntity.ok().build();
    }

    // Get lamps of user
    @GetMapping("/me/lamps")
    public ResponseEntity<List<LampDto>> getMyLamps(Authentication authentication) {
        // Spring Security bring here UserDetails after JWT verification
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<LampDto> lamps = user.getLamps().stream()
                .map(lamp -> new LampDto(
                        lamp.getId(),
                        lamp.isOn(),
                        (lamp.getFleet() != null) ? lamp.getFleet().getId() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(lamps);
    }

    // Add lamp
    @PostMapping("/me/lamps")
    public ResponseEntity<Void> addLamp(@RequestBody AddLampDto dto, Authentication authentication) {
        lampService.assignLampToUser(authentication.getName(), dto.getLampId());
        return ResponseEntity.ok().build();
    }

    // Delete lamp from account (DELETE /users/me/lamps/{lampId})
    @DeleteMapping("/me/lamps/{lampId}")
    public ResponseEntity<Void> removeLamp(@PathVariable String lampId, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        // Check if lamp is assigned to user
        Lamp lamp = lampRepository.findById(lampId).orElseThrow();
        if (lamp.getOwner() != null && lamp.getOwner().getId().equals(user.getId())) {
            lamp.setOwner(null);
            lampRepository.save(lamp);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build();
    }
}