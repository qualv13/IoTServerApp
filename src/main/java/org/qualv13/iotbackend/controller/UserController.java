package org.qualv13.iotbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Operation(summary = "Rejestracja użytkownika", description = "Tworzy nowe konto w systemie. Login musi być unikalny.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie zarejestrowano"),
            @ApiResponse(responseCode = "400", description = "Nazwa użytkownika jest już zajęta")
    })
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
    @Operation(summary = "Zmiana nazwy użytkownika", description = "Pozwala zalogowanemu użytkownikowi zmienić swój login.")
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
    @Operation(summary = "Zmiana hasła", description = "Wymaga podania starego hasła dla weryfikacji.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hasło zmienione"),
            @ApiResponse(responseCode = "400", description = "Stare hasło jest nieprawidłowe")
    })
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
    @Operation(summary = "Usuwanie konta", description = "Trwale usuwa konto użytkownika. Lampy zostają odpięte (nieusunięte).")
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
    @Operation(summary = "Pobierz moje lampy", description = "Zwraca listę lamp przypisanych do zalogowanego użytkownika.")
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
    @Operation(summary = "Przypisz lampę", description = "Przypisuje istniejącą lampę do konta użytkownika i generuje nowy Device Token.")
    @PostMapping("/me/lamps")
    public ResponseEntity<Map<String, String>> addLamp(@RequestBody AddLampDto dto, Authentication authentication) {
        // Metoda serwisu zwraca teraz String (token)
        String newToken = lampService.assignLampToUser(authentication.getName(), dto.getLampId());

        // Zwracamy go w JSONie
        return ResponseEntity.ok(Collections.singletonMap("device_token", newToken));
    }

    // Delete lamp from account (DELETE /users/me/lamps/{lampId})
    @Operation(summary = "Odepnij lampę", description = "Usuwa powiązanie lampy z użytkownikiem (lampa staje się niczyja).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lampa odpięta"),
            @ApiResponse(responseCode = "403", description = "Próba usunięcia cudzej lampy")
    })
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