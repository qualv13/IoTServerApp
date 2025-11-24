package org.qualv13.iotbackend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}