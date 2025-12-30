package org.qualv13.iotbackend.controller;

import com.iot.backend.proto.IotProtos;
import org.qualv13.iotbackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
public class OtaController {

    private final MqttService mqttService;
    private static final String UPLOAD_DIR = "firmware_uploads/";

    // Endpoint: POST /ota/upload?lampId=xyz (lub fleetId)
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFirmware(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(required = false) String lampId,
                                                 @RequestParam(required = false) Long fleetId) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");

        try {
            // 1. Zapisz plik na dysku serwera
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.write(path, file.getBytes());

            // 2. Wygeneruj URL (w realnym scenariuszu to byłby publiczny IP serwera)
            String downloadUrl = "http://localhost:8080/ota/download/" + filename;

            // 3. Stwórz komendę Protobuf
            IotProtos.LampCommand otaCommand = IotProtos.LampCommand.newBuilder()
                    .setType(IotProtos.LampCommand.Type.OTA_UPDATE)
                    .setOtaUrl(downloadUrl)
                    .build();

            // 4. Wyślij powiadomienie do urządzeń
            if (lampId != null) {
                mqttService.sendCommandToLamp(lampId, otaCommand);
            } else if (fleetId != null) {
                mqttService.sendCommandToFleet(fleetId, otaCommand);
            }

            return ResponseEntity.ok("OTA started. URL sent: " + downloadUrl);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping(value = "/proto-definition", produces = "text/plain")
    public String getProtoDefinition() {
        try {
            // Odczytaj plik z resources (musi być w src/main/resources/iot_service.proto)
            Resource resource = new ClassPathResource("proto/iot_service.proto");
            byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return new String(bdata, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ""; // Lub rzuć wyjątek
        }
    }
}