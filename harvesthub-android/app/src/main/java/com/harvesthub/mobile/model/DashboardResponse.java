package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class DashboardResponse {
    @SerializedName("farmerName")
    private String farmerName;
    @SerializedName("totalRevenue")
    private String totalRevenue;
    @SerializedName("revenueChange")
    private String revenueChange;
    @SerializedName("previousRevenue")
    private String previousRevenue;
    @SerializedName("activeListings")
    private String activeListings;
    @SerializedName("completedOrders")
    private String completedOrders;
    @SerializedName("trustScore")
    private String trustScore;

    public String getFarmerName() { return farmerName; }
    public String getTotalRevenue() { return totalRevenue; }
    public String getRevenueChange() { return revenueChange; }
    public String getPreviousRevenue() { return previousRevenue; }
    public String getActiveListings() { return activeListings; }
    public String getCompletedOrders() { return completedOrders; }
    public String getTrustScore() { return trustScore; }

    // newer fields (real data from DashboardService)
    private String greeting;
    private int trustPercent;
    private Double ratingAverage;
    private int ratingCount;
    private String location;
    private Double latitude;
    private Double longitude;

    public String getGreeting() { return greeting; }
    public int getTrustPercent() { return trustPercent; }
    public Double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
    public String getLocation() { return location; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
