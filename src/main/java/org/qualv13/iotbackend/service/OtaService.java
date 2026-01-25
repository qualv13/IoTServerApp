package org.qualv13.iotbackend.service;

import com.iot.backend.proto.IotProtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qualv13.iotbackend.dto.OtaCheckResponse; // Upewnij się że masz to DTO (z poprzedniej wiadomości)
import org.qualv13.iotbackend.entity.FirmwareRelease;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.repository.FirmwareRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtaService {

    private final S3Client s3Client;
    private final FirmwareRepository firmwareRepository;
    private final LampRepository lampRepository;
    private final MqttService mqttService;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.public-url}")
    private String publicUrlBase;

    // --- ADMIN: Upload ---
    @Transactional
    public FirmwareRelease uploadFirmware(String version, MultipartFile file, boolean isPublished) throws IOException {
        String key = "firmware/" + version + "/" + file.getOriginalFilename();

        log.info("Wysyłanie firmware {} do R2 (klucz: {}) na url {}, published {}", version, key, publicUrlBase, isPublished);

        // Upload do R2
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/octet-stream")
                        .build(),
                RequestBody.fromBytes(file.getBytes()));

        // Zapis w bazie
        FirmwareRelease release = new FirmwareRelease();
        release.setVersion(version);
        release.setFilename(file.getOriginalFilename());
        release.setDownloadUrl(publicUrlBase + key);
        release.setS3Key(key);
        release.setCreatedAt(LocalDateTime.now());
        release.setPublished(isPublished);

        return firmwareRepository.save(release);
    }

    // --- ADMIN: Listowanie wersji ---
    public List<FirmwareRelease> getAllFirmwares() {
        return firmwareRepository.findAllByOrderByCreatedAtDesc();
    }

    // --- ADMIN: Publikowanie / Ukrywanie istniejącej wersji ---
    // Przydatne, jeśli najpierw wrzucisz jako ukryte, a potem chcesz opublikować
    @Transactional
    public FirmwareRelease togglePublish(Long id, boolean isPublished) {
        FirmwareRelease release = firmwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wersja nie istnieje"));

        release.setPublished(isPublished);
        return firmwareRepository.save(release);
    }

    // --- ADMIN: Usuwanie wersji (Baza + S3) ---
    @Transactional
    public void deleteFirmware(Long id) {
        FirmwareRelease release = firmwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wersja nie istnieje"));

        // 1. Usuń plik z chmury (R2)
        try {
            if (release.getS3Key() != null) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(release.getS3Key())
                        .build());
                log.info("Usunięto plik z R2: {}", release.getS3Key());
            }
        } catch (Exception e) {
            log.error("Nie udało się usunąć pliku z R2: {}", e.getMessage());
            // Kontynuujemy usuwanie z bazy mimo błędu S3
        }

        // 2. Usuń rekord z bazy
        firmwareRepository.delete(release);
    }

    // --- USER: Sprawdzenie dostępności ---
    public OtaCheckResponse checkForUpdate(String lampId) {
        Lamp lamp = lampRepository.findById(lampId).orElseThrow();
        String currentVer = lamp.getFirmwareVersion();

        // Pobierz najnowszy PUBLIKOWANY firmware
        Optional<FirmwareRelease> latestOpt = firmwareRepository.findTopByIsPublishedTrueOrderByCreatedAtDesc();

        if (latestOpt.isEmpty()) {
            return new OtaCheckResponse(false, currentVer, null, null);
        }

        FirmwareRelease latest = latestOpt.get();

        // Proste porównanie: jeśli stringi są różne, to jest update
        // (W produkcji lepiej użyć biblioteki SemVer np. com.github.zafarkhaja:java-semver)
        boolean updateAvailable = currentVer == null || !currentVer.equals(latest.getVersion());

        return new OtaCheckResponse(
                updateAvailable,
                currentVer,
                latest.getVersion(),
                latest.getDownloadUrl()
        );
    }

    // --- USER: Wykonanie aktualizacji ---
    public void triggerUpdate(String lampId) {
        FirmwareRelease latest = firmwareRepository.findTopByIsPublishedTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new RuntimeException("Brak dostępnego firmware"));

        log.info("Wysyłanie komendy OTA do lampy {}. URL: {}", lampId, latest.getDownloadUrl());

        IotProtos.DownloadOtaUpdateCommand otaCmd = IotProtos.DownloadOtaUpdateCommand.newBuilder()
                .setOtaUrl(latest.getDownloadUrl())
                .build();

        IotProtos.LampCommand command = IotProtos.LampCommand.newBuilder()
                .setVersion(1)
                .setTs(System.currentTimeMillis() / 1000)
                .setDownloadOtaUpdateCommand(otaCmd)
                .build();

        mqttService.sendCommandToLamp(lampId, command);
    }
}