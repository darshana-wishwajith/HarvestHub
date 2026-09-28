package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors POST /api/auth/login's response body. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponseDto {
    public String token;
    public OfficerDto officer;
}
