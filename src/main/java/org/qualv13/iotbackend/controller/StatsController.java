package org.qualv13.iotbackend.controller;

import org.qualv13.iotbackend.dto.StatsDto;
import org.qualv13.iotbackend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // Endpoint dla Admina (Globalne dane)
    @GetMapping("/global")
    public ResponseEntity<StatsDto> getGlobalStats(Authentication auth) {
        // PROSTY SEC CHECK: W prawdziwej aplikacji użyłbyś ról (ROLE_ADMIN).
        // Tutaj dla uproszczenia zakładamy, że user "admin" ma dostęp.
        if (!auth.getName().equals("admin")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(statsService.getGlobalStats());
    }

    // Endpoint dla Użytkownika (Jego dane)
    @GetMapping("/me")
    public ResponseEntity<StatsDto> getMyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getUserStats(auth.getName()));
    }
}