package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors the backend's OfficerDto JSON shape. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficerDto {
    public Long id;
    public String username;
    public String fullName;
    public String officerIdCode;
    public String region;
    public String email;
    public String phone;
    public String role;
}
