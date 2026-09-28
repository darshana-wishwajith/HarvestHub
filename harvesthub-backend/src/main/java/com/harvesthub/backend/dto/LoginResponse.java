package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserDto user;
    private OfficerDto officer;

    public LoginResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }
}
