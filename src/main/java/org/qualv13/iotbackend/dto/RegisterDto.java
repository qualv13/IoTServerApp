package org.qualv13.iotbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RegisterDto {
    @Schema(description = "Nazwa użytkownika", example = "jan_kowalski", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Hasło do konta", example = "tajneHaslo123", minLength = 6)
    private String password;
}
