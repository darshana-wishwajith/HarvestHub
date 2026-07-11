package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class WantedAd {
    @SerializedName("requestId")
    private String requestId;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("location")
    private String location;

    @SerializedName("requiredQuantityKg")
    private double requiredQuantityKg;

    @SerializedName("targetPricePerKg")
    private double targetPricePerKg;

    @SerializedName("isUrgent")
    private boolean isUrgent;

    public WantedAd(String requestId, String cropName, String location, double requiredQuantityKg, double targetPricePerKg, boolean isUrgent) {
        this.requestId = requestId;
        this.cropName = cropName;
        this.location = location;
        this.requiredQuantityKg = requiredQuantityKg;
        this.targetPricePerKg = targetPricePerKg;
        this.isUrgent = isUrgent;
    }

    public String getRequestId() { return requestId; }
    public String getCropName() { return cropName; }
    public String getLocation() { return location; }
    public double getRequiredQuantityKg() { return requiredQuantityKg; }
    public double getTargetPricePerKg() { return targetPricePerKg; }
    public boolean isUrgent() { return isUrgent; }
}
