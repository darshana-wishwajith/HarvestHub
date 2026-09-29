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

    @SerializedName("buyerId")
    private String buyerId;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("deadline")
    private String deadline;

    @SerializedName("targetGrade")
    private String targetGrade;

    @SerializedName("status")
    private String status;

    public WantedAd() {
    }

    public WantedAd(String requestId, String cropName, String location, double requiredQuantityKg, double targetPricePerKg, boolean isUrgent) {
        this.requestId = requestId;
        this.cropName = cropName;
        this.location = location;
        this.requiredQuantityKg = requiredQuantityKg;
        this.targetPricePerKg = targetPricePerKg;
        this.isUrgent = isUrgent;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getRequiredQuantityKg() { return requiredQuantityKg; }
    public void setRequiredQuantityKg(double requiredQuantityKg) { this.requiredQuantityKg = requiredQuantityKg; }

    public double getTargetPricePerKg() { return targetPricePerKg; }
    public void setTargetPricePerKg(double targetPricePerKg) { this.targetPricePerKg = targetPricePerKg; }

    public boolean isUrgent() { return isUrgent; }
    public void setUrgent(boolean urgent) { isUrgent = urgent; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getTargetGrade() { return targetGrade; }
    public void setTargetGrade(String targetGrade) { this.targetGrade = targetGrade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
