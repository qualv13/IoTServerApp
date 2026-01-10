package org.qualv13.iotbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.repository.LampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@RestController
@RequestMapping("/api/mqtt/auth")
@RequiredArgsConstructor
public class RabbitAuthController {

    private final LampRepository lampRepository;

    // Pobieramy dane "admina" (czyli Twojego serwera) z application.yaml
    @Value("${mqtt.username}")
    private String serverUsername;
    @Value("${mqtt.password}")
    private String serverPassword;

    // 1. Autoryzacja Użytkownika (Sprawdzenie hasła)
    @PostMapping("/user")
    public ResponseEntity<String> checkUser(@RequestParam("username") String username,
                                            @RequestParam("password") String password) { // password = token od lampy

        if (serverUsername != null && serverUsername.equals(username)) {
            // Sprawdzamy czy hasło backendu zgadza się z tym w configu
            if (serverPassword != null && serverPassword.equals(password)) {
                log.info("AUTH SUCCESS: Backend service authenticated.");
                return ResponseEntity.ok("allow");
            } else {
                log.warn("AUTH FAILED: Backend password mismatch.");
                return ResponseEntity.status(403).body("deny");
            }
        }

        return lampRepository.findById(username)
                .map(lamp -> {
                    // Hashujemy to co przysłała lampa
                    String incomingTokenHash = calculateSha256(password);

                    // Porównujemy hashe
                    if (lamp.getDeviceTokenHash() != null && lamp.getDeviceTokenHash().equals(incomingTokenHash)) {
                        return ResponseEntity.ok("allow");
                    }
                    log.warn("AUTH RABBIT FAILED: For user {}.", username);
                    return ResponseEntity.status(403).body("deny");
                })
                .orElse(ResponseEntity.status(403).body("deny"));
    }

    // 2. Autoryzacja VHost (Zazwyczaj pozwalamy na wszystko w domyślnym vhost)
    @PostMapping("/vhost")
    public ResponseEntity<String> checkVhost(@RequestParam("username") String username,
                                             @RequestParam("vhost") String vhost) {
        return ResponseEntity.ok("allow");
    }

    // 3. Autoryzacja Zasobów (Topiców) - Opcjonalne zabezpieczenie
    @PostMapping("/resource")
    public ResponseEntity<String> checkResource(@RequestParam("username") String username,
                                                @RequestParam("resource") String resource, // exchange, queue, topic
                                                @RequestParam("name") String name,
                                                @RequestParam("permission") String permission) {

        log.info("RabbitMQ Auth Resource: User={}, Res={}, Name={}, Perm={}", username, resource, name, permission);

        // Admin (Serwer) może wszystko
        if (username.equals(serverUsername)) return ResponseEntity.ok("allow");

        // Lampa może pisać/czytać tylko swoje tematy?
        // Na razie dla uproszczenia pozwalamy na wszystko
        return ResponseEntity.ok("allow");
    }

    // 4. Autoryzacja Tematów (Topic Authorization)
    @PostMapping("/topic")
    public ResponseEntity<String> checkTopic(
            @RequestParam("username") String username,
            @RequestParam("vhost") String vhost,
            @RequestParam("resource") String resource, // will be "topic"
            @RequestParam("name") String name,         // exchange name
            @RequestParam("permission") String permission, // "write" or "read"
            @RequestParam("routing_key") String routingKey
    ) {
        // Log basic info for debugging
        System.out.println("Checking topic auth for user: " + username + " on topic: " + routingKey);
        log.info("Checking topic auth for: User={}, topic={}, Name={}, Perm={}", username, routingKey, name, permission);

        // Return "allow" as plain text with 200 OK
        return ResponseEntity.ok("allow");
    }

    private String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Błąd hashowania tokena", e);
        }
    }
}