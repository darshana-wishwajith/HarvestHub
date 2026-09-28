package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ConfirmDealRequest {
    @SerializedName("quantity")
    private double quantity;
    
    @SerializedName("pricePerKg")
    private double pricePerKg;
    
    @SerializedName("deliveryDate")
    private String deliveryDate;

    @SerializedName("status")
    private String status;

    @SerializedName("note")
    private String note;

    public ConfirmDealRequest(double quantity, double pricePerKg, String deliveryDate) {
        this.quantity = quantity;
        this.pricePerKg = pricePerKg;
        this.deliveryDate = deliveryDate;
    }

    public ConfirmDealRequest(String status, String note) {
        this.status = status;
        this.note = note;
    }

    public double getQuantity() { return quantity; }
    public double getPricePerKg() { return pricePerKg; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
}
