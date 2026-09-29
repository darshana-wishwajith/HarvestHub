package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class SupplyOffer {
    @SerializedName("id")
    private String id;

    @SerializedName("requestId")
    private String requestId;

    @SerializedName("batchId")
    private String batchId;

    @SerializedName("farmerId")
    private String farmerId;

    @SerializedName("farmerName")
    private String farmerName;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("location")
    private String location;

    @SerializedName("grade")
    private String grade;

    @SerializedName("quantity")
    private double quantity;

    @SerializedName("pricePerKg")
    private double pricePerKg;

    @SerializedName("status")
    private String status;

    @SerializedName("createdDate")
    private String createdDate;

    public SupplyOffer() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
}
