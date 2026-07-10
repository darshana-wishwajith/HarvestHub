package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class DashboardResponse {
    @SerializedName("totalRevenue")
    private String totalRevenue;

    public String getTotalRevenue() { return totalRevenue; }
}
