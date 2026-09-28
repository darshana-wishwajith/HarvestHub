package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerDto {
    private String id;
    private String username;
    private String fullName;
    private String officerIdCode;
    private String region;
    private String email;
    private String phone;
    private String role;
}
