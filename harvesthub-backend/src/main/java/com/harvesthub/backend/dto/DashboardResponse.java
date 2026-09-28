package com.harvesthub.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String farmerName;
    private String totalRevenue;
    private String revenueChange;
    private String previousRevenue;
    private String activeListings;
    private String completedOrders;
    private String trustScore;
}
