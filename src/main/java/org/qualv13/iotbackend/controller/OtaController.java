package org.qualv13.iotbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.OtaCheckResponse;
import org.qualv13.iotbackend.entity.FirmwareRelease;
import org.qualv13.iotbackend.service.OtaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
@Slf4j
public class OtaController {

    private final OtaService otaService;

    // --- ADMIN: Upload nowego firmware ---
    @Operation(summary = "Admin: Upload Firmware (.bin)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FirmwareRelease> uploadFirmware(
            @RequestParam("version") String version,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "published", defaultValue = "false") boolean published,
            Authentication auth) throws IOException {

        // Check admin
        if (!"admin".equalsIgnoreCase(auth.getName())) return ResponseEntity.status(403).build();

        return ResponseEntity.ok(otaService.uploadFirmware(version, file, published));
    }

    // --- ADMIN: Lista wersji ---
    @Operation(summary = "Admin: Lista wszystkich wersji firmware")
    @GetMapping("/admin/list")
    public ResponseEntity<List<FirmwareRelease>> listFirmwares(Authentication auth) {
        if (!"admin".equalsIgnoreCase(auth.getName())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(otaService.getAllFirmwares());
    }

    // --- ADMIN: Usuń wersję ---
    @Operation(summary = "Admin: Usuń wersję (np. zbugowaną)")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteFirmware(@PathVariable Long id, Authentication auth) {
        if (!"admin".equalsIgnoreCase(auth.getName())) return ResponseEntity.status(403).build();

        otaService.deleteFirmware(id);
        log.info("Admin usunął firmware o ID: {}", id);
        return ResponseEntity.ok().build();
    }

    // --- ADMIN: Zmień status publikacji (np. Opublikuj wersję testową) ---
    @Operation(summary = "Admin: Zmień status publikacji (Opublikuj/Ukryj)")
    @PatchMapping("/admin/{id}/publish")
    public ResponseEntity<FirmwareRelease> setPublishStatus(
            @PathVariable Long id,
            @RequestParam boolean published,
            Authentication auth) {

        if (!"admin".equalsIgnoreCase(auth.getName())) return ResponseEntity.status(403).build();

        return ResponseEntity.ok(otaService.togglePublish(id, published));
    }

    // --- USER: Sprawdź czy jest update ---
    @Operation(summary = "Sprawdź dostępność aktualizacji dla lampy")
    @GetMapping("/lamps/{lampId}/check")
    public ResponseEntity<OtaCheckResponse> checkUpdate(@PathVariable String lampId) {
        return ResponseEntity.ok(otaService.checkForUpdate(lampId));
    }

    // --- USER: WYKONAJ aktualizację ---
    @Operation(summary = "Zleć aktualizację lampy do najnowszej wersji")
    @PostMapping("/lamps/{lampId}/trigger")
    public ResponseEntity<Void> triggerUpdate(@PathVariable String lampId) {
        // Tutaj można dodać weryfikację właściciela (getLampWithAuthCheck w serwisie)
        otaService.triggerUpdate(lampId);
        return ResponseEntity.ok().build();
    }
}
//package org.qualv13.iotbackend.controller;
//
//import com.iot.backend.proto.IotProtos;
//import lombok.extern.slf4j.Slf4j;
//import org.qualv13.iotbackend.repository.LampRepository;
//import org.qualv13.iotbackend.service.MqttService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Instant;
//
//@Slf4j
//@RestController
//@RequestMapping("/ota")
//@RequiredArgsConstructor
//public class OtaController {
//
//    private final MqttService mqttService;
//    private final LampRepository lampRepository;
//
//    // Przykład endpointu do aktualizacji pojedynczej lampy
//    @PostMapping("/lamps/{lampId}")
//    public ResponseEntity<Void> updateLamp(@PathVariable String lampId, @RequestParam String url, Authentication auth) {
//        log.info("POST /ota/lamps/{} for url {}", lampId, url);
//
//        // Admin only check
//        if (!"admin".equals(auth.getName())) {
//            return ResponseEntity.status(403).build();
//        }
//
//        // 1. Budujemy komendę OTA
//        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
//                .setOtaUrl(url)
//                .build();
//
//        // 2. Pakujemy w LampCommand (używając oneof)
//        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
//                .setVersion(1)
//                .setTs(Instant.now().getEpochSecond())
//                .setDownloadOtaUpdateCommand(otaCmd)
//                .build();
//
//        // 3. Wysyłamy
//        mqttService.sendCommandToLamp(lampId, command);
//
//        return ResponseEntity.ok().build();
//    }
//
//    // Przykład endpointu do aktualizacji FLOTY
//    @PostMapping("/fleets/{fleetId}")
//    public ResponseEntity<Void> updateFleet(@PathVariable Long fleetId, @RequestParam String url, Authentication auth) {
//        log.info("POST /ota/fleets/{} for url {}", fleetId, url);
//
//        // Admin only check
//        if (!"admin".equals(auth.getName())) {
//            return ResponseEntity.status(403).build();
//        }
//
//        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
//                .setOtaUrl(url)
//                .build();
//
//        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
//                .setVersion(1)
//                .setTs(Instant.now().getEpochSecond())
//                .setDownloadOtaUpdateCommand(otaCmd)
//                .build();
//
//        lampRepository.findAll().forEach(lamp -> {
//            mqttService.sendCommandToLamp(lamp.getId(), command);
//        });
//        //mqttService.sendCommandToFleet(fleetId, command);
//
//        return ResponseEntity.ok().build();
//    }
//
//    // Nowy endpoint: OTA dla wszystkich lamp (bez floty)
//    @PostMapping("/lamps")
//    public ResponseEntity<Void> updateAllLamps(@RequestParam String url, Authentication auth) {
//        log.info("POST /ota/lamps (ALL) for url {}", url);
//
//        // Admin only check
//        if (!"admin".equals(auth.getName())) {
//            return ResponseEntity.status(403).build();
//        }
//
//        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
//                .setOtaUrl(url)
//                .build();
//
//        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
//                .setVersion(1)
//                .setTs(Instant.now().getEpochSecond())
//                .setDownloadOtaUpdateCommand(otaCmd)
//                .build();
//
//        lampRepository.findAll().forEach(lamp -> mqttService.sendCommandToLamp(lamp.getId(), command));
//
//        return ResponseEntity.ok().build();
//    }
//}