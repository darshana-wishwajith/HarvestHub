package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class FarmerSummaryResponse {
    @SerializedName("monthlyRevenue")
    private String monthlyRevenue;

    @SerializedName("trustScore")
    private String trustScore;

    public FarmerSummaryResponse(String monthlyRevenue, String trustScore) {
        this.monthlyRevenue = monthlyRevenue;
        this.trustScore = trustScore;
    }

    public String getMonthlyRevenue() { return monthlyRevenue; }
    public String getTrustScore() { return trustScore; }

    private int trustPercent;
    private Double ratingAverage;

    public int getTrustPercent() { return trustPercent; }
    public Double getRatingAverage() { return ratingAverage; }
}
