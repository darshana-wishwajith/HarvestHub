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
}
