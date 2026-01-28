package org.qualv13.iotbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.*;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.LampMetric;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampMetricRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import org.qualv13.iotbackend.service.LampService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LampService lampService;
    private final LampRepository lampRepository;
    private final LampMetricRepository metricRepository;

    public record MessageResponse(String message) {}

    @Operation(summary = "Pobierz mój profil (Check Token)", description = "Zwraca podstawowe dane zalogowanego użytkownika. Służy do weryfikacji ważności tokena przy starcie aplikacji.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token jest ważny, zwraca profil"),
            @ApiResponse(responseCode = "401", description = "Token wygasł lub jest nieprawidłowy - wymuś wylogowanie")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyProfile(Authentication auth) {
        log.info("GET /users/me");
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserDto userDto = new UserDto();
        userDto.setUsername(user.getUsername());
        userDto.setId(Long.toString(user.getId()));

        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Rejestracja użytkownika", description = "Tworzy nowe konto w systemie. Login musi być unikalny.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie zarejestrowano"),
            @ApiResponse(responseCode = "400", description = "Nazwa użytkownika jest już zajęta")
    })
    @PostMapping
    public ResponseEntity<MessageResponse> registerUser(@RequestBody RegisterDto dto) {
        log.info("POST /users");
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            log.warn("Username already exists");
            return ResponseEntity.badRequest().body(new MessageResponse("Username already exists"));
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);
        log.info("Username registered successfully -> saved to database");
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    // Username change (PUT /users/me)
    @Operation(summary = "Zmiana nazwy użytkownika", description = "Pozwala zalogowanemu użytkownikowi zmienić swój login.")
    @PutMapping("/me")
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserDto dto, Authentication auth) {
        log.info("PUT /users/me");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
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
    public ResponseEntity<MessageResponse> changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        log.info("PUT /users/me/password");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Incorrect current password"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Password changed"));
    }

    // Delete account (DELETE /users/me)
    @Operation(summary = "Usuwanie konta", description = "Trwale usuwa konto użytkownika. Lampy zostają odpięte (nieusunięte).")
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        log.info("DELETE /users/me");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

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
        // log.info("GET /users/me/lamps");
        try {
            String username = authentication.getName();
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(120);
            List<Lamp> lampsToProcess;

            if ("admin".equals(username)) {
                lampsToProcess = lampRepository.findAll();
            } else {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null) return ResponseEntity.ok(Collections.emptyList());
                lampsToProcess = user.getLamps();
            }

            if (lampsToProcess == null || lampsToProcess.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<LampDto> result = new ArrayList<>();

            for (Lamp lamp : lampsToProcess) {
                try {
                    boolean isOnline = false;

                    if (lamp.getId() != null) {
                        Optional<LampMetric> lastMetric = metricRepository.findFirstByLampIdOrderByTimestampDesc(lamp.getId());
                        if (lastMetric.isPresent() && lastMetric.get().getTimestamp() != null) {
                            isOnline = lastMetric.get().getTimestamp().isAfter(threshold);
                        }
                    }

                    Long fleetId = (lamp.getFleet() != null) ? lamp.getFleet().getId() : null;

                    result.add(new LampDto(
                            lamp.getId(),
                            lamp.isOn(),
                            lamp.getDeviceName(),
                            lamp.isOnline(),
                            fleetId
                    ));
                } catch (Exception e) {
                    log.error("Skipping lamp {} due to error: {}", lamp.getId(), e.getMessage());
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("CRITICAL /users/me/lamps: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Add lamp
    @Operation(summary = "Przypisz lampę", description = "Przypisuje istniejącą lampę do konta użytkownika i generuje nowy Device Token.")
    @PostMapping("/me/lamps")
    public ResponseEntity<Map<String, String>> addLamp(@RequestBody AddLampDto dto, Authentication authentication) {
        log.info("POST /users/me/lamps");
        String newToken = lampService.assignLampToUser(authentication.getName(), dto.getLampId());
        log.info("Lamp Id: {}", dto.getLampId());
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
        log.info("DELETE /users/me/lamps/{lampId}");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        Lamp lamp = lampRepository.findById(lampId).orElseThrow();

        if(auth.getName().equals("admin")) {
            lampRepository.delete(lamp);
            return ResponseEntity.ok().build();
        }

        if (lamp.getOwner() != null && lamp.getOwner().getId().equals(user.getId())) {
            lamp.setOwner(null);
            lampRepository.save(lamp);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build();
    }
}