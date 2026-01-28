package org.qualv13.iotbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.DetailedStatsDto;
import org.qualv13.iotbackend.dto.LampHistoryDto;
import org.qualv13.iotbackend.dto.StatsDto;
import org.qualv13.iotbackend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/global")
    public ResponseEntity<DetailedStatsDto> getGlobalStats(Authentication auth) {
        log.info("GET /stats/global");
        if (!auth.getName().equals("admin")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(statsService.getGlobalStats());
    }

    // Endpoint dla Użytkownika
    @GetMapping("/me")
    public ResponseEntity<DetailedStatsDto> getMyStats(Authentication auth) {
        log.info("GET /stats/me");
        return ResponseEntity.ok(statsService.getUserStats(auth.getName()));
    }

    // Statystyki konkretnej floty
    @GetMapping("/fleets/{fleetId}")
    public ResponseEntity<DetailedStatsDto> getFleetStats(@PathVariable Long fleetId, Authentication auth) {
        return ResponseEntity.ok(statsService.getFleetStats(fleetId));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<DetailedStatsDto> getUserStatsByAdmin(@PathVariable String username, Authentication auth) {
        if (!"admin".equals(auth.getName())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(statsService.getStatsForUserByAdmin(username));
    }

    // Historia pojedynczej lampy
    @GetMapping("/lamps/{lampId}/history")
    public ResponseEntity<LampHistoryDto> getLampHistory(@PathVariable String lampId) {
        return ResponseEntity.ok(statsService.getSingleLampHistory(lampId));
    }
}