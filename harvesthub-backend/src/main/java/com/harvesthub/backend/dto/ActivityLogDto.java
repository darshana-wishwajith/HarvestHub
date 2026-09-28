package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto {
    private String id;
    private String cropName;
    private String variety;
    private String farmerName;
    private String location;
    private String grade;
    private String status;
    private String timestamp;
}
