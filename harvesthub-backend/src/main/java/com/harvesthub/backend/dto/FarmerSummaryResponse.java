package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Header of My Listings. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmerSummaryResponse {
    private String monthlyRevenue;        // "LKR 42,500"
    private String trustScore;            // "94" (percent)
    private int trustPercent;
    private Double ratingAverage;
}
