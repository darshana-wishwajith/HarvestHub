package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class SupplyOfferRequest {
    @SerializedName("requestId")
    private String requestId;

    @SerializedName("batchId")
    private String batchId;

    @SerializedName("quantity")
    private double quantity;

    @SerializedName("pricePerKg")
    private double pricePerKg;

    public SupplyOfferRequest(String requestId, String batchId, double quantity, double pricePerKg) {
        this.requestId = requestId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.pricePerKg = pricePerKg;
    }

    public String getRequestId() { return requestId; }
    public String getBatchId() { return batchId; }
    public double getQuantity() { return quantity; }
    public double getPricePerKg() { return pricePerKg; }
}
