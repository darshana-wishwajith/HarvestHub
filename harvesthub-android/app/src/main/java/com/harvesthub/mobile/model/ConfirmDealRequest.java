package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ConfirmDealRequest {
    @SerializedName("quantity")
    private double quantity;
    
    @SerializedName("pricePerKg")
    private double pricePerKg;
    
    @SerializedName("deliveryDate")
    private String deliveryDate;

    public ConfirmDealRequest(double quantity, double pricePerKg, String deliveryDate) {
        this.quantity = quantity;
        this.pricePerKg = pricePerKg;
        this.deliveryDate = deliveryDate;
    }

    public double getQuantity() { return quantity; }
    public double getPricePerKg() { return pricePerKg; }
    public String getDeliveryDate() { return deliveryDate; }
}
