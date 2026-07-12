package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class HistoryListing {
    @SerializedName("listingId")
    private String listingId;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("status")
    private String status; // "DELIVERED" or "CLOSED"

    @SerializedName("date")
    private String date;

    @SerializedName("buyerName")
    private String buyerName;

    @SerializedName("quantityKg")
    private String quantityKg;

    @SerializedName("totalValue")
    private String totalValue;

    @SerializedName("finalPricePerKg")
    private String finalPricePerKg;

    public HistoryListing(String listingId, String cropName, String status, String date, String buyerName, String quantityKg, String totalValue, String finalPricePerKg) {
        this.listingId = listingId;
        this.cropName = cropName;
        this.status = status;
        this.date = date;
        this.buyerName = buyerName;
        this.quantityKg = quantityKg;
        this.totalValue = totalValue;
        this.finalPricePerKg = finalPricePerKg;
    }

    public String getListingId() { return listingId; }
    public String getCropName() { return cropName; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getBuyerName() { return buyerName; }
    public String getQuantityKg() { return quantityKg; }
    public String getTotalValue() { return totalValue; }
    public String getFinalPricePerKg() { return finalPricePerKg; }
}
