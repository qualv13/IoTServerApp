package org.qualv13.iotbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtaCheckResponse {
    private boolean updateAvailable;
    private String currentVersion;
    private String latestVersion;
    private String downloadUrl;
}