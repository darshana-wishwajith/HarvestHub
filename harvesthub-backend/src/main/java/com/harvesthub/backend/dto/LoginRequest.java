package com.harvesthub.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String emailOrMobile;
    private String username;
    private String password;

    public String getEmailOrMobile() {
        if (emailOrMobile != null && !emailOrMobile.isBlank()) {
            return emailOrMobile;
        }
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        if (this.emailOrMobile == null || this.emailOrMobile.isBlank()) {
            this.emailOrMobile = username;
        }
    }
}
