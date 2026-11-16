package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Farmer Home (Figma 10). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String farmerName;
    private String greeting;
    private String totalRevenue;          // this month, "125,000"
    private String revenueChange;         // "+12%" vs last month, null when no data
    private String previousRevenue;       // last month
    private String activeListings;
    private String completedOrders;
    private String trustScore;            // "94" (percent)
    private int trustPercent;
    private Double ratingAverage;
    private int ratingCount;
    private String location;              // farm address
    private Double latitude;
    private Double longitude;
}
