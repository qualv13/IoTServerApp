package org.qualv13.iotbackend.controller;

import org.qualv13.iotbackend.dto.AddLampDto;
import org.qualv13.iotbackend.dto.RegisterDto;
import org.qualv13.iotbackend.entity.User;
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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LampService lampService;

    @PostMapping
    public ResponseEntity<String> registerUser(@RequestBody RegisterDto dto) {
        // Sprawdź czy user już istnieje
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        // Tworzymy encję User
        User user = new User();
        user.setUsername(dto.getUsername());
        // Hasło musi być zahashowane
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    // Pobieranie lamp zalogowanego użytkownika
    @GetMapping("/me/lamps")
    public ResponseEntity<List<LampDto>> getMyLamps(Authentication authentication) {
        // Spring Security automatycznie wstrzykuje tu obiekt UserDetails po weryfikacji JWT
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<LampDto> lamps = user.getLamps().stream()
                .map(lamp -> new LampDto(lamp.getId(), true)) // Mockujemy status "true" na razie
                .collect(Collectors.toList());

        return ResponseEntity.ok(lamps);
    }

    // Dodawanie lampy
    @PostMapping("/me/lamps")
    public ResponseEntity<Void> addLamp(@RequestBody AddLampDto dto, Authentication authentication) {
        lampService.assignLampToUser(authentication.getName(), dto.getLampId());
        return ResponseEntity.ok().build();
    }
}