package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class CropBatch {
    @SerializedName("id")
    private String id;

    @SerializedName("batchNumber")
    private String batchNumber;

    @SerializedName("availableQuantity")
    private double availableQuantity;

    @SerializedName("isVerified")
    private boolean isVerified;

    public CropBatch(String id, String batchNumber, double availableQuantity, boolean isVerified) {
        this.id = id;
        this.batchNumber = batchNumber;
        this.availableQuantity = availableQuantity;
        this.isVerified = isVerified;
    }

    public String getId() { return id; }
    public String getBatchNumber() { return batchNumber; }
    public double getAvailableQuantity() { return availableQuantity; }
    public boolean isVerified() { return isVerified; }
}
