package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueListingDto {
    private String id;
    private String cropName;
    private String farmerName;
    private String location;
    private String submittedAgo;
    private String quantity;
    private String pricePerKg;
    private String statusLabel;
    private boolean claimable;
    private String tone;
    private String iconLiteral;
}
