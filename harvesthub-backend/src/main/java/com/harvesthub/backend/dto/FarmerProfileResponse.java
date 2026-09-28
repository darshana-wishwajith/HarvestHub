package com.harvesthub.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmerProfileResponse {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String nic;
    private String profilePhotoUrl;
    private String passwordLastUpdated;
}
