package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class Order {
    @SerializedName("orderId")
    private String id;
    @SerializedName("farmerId")
    private String farmerId;
    @SerializedName("buyerId")
    private String buyerId;
    @SerializedName("listingId")
    private String listingId;
    @SerializedName("quantity")
    private double quantity;
    @SerializedName("pricePerKg")
    private double pricePerKg;
    @SerializedName("deliveryDate")
    private String deliveryDate;
    private String status;
    @SerializedName("createdDate")
    private String createdAt;
    private double totalAmount;
    private String cropName;
    private String farmerName;
    private String escrowStatus;
    private String transporterId;

    public Order() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public String getEscrowStatus() { return escrowStatus; }
    public void setEscrowStatus(String escrowStatus) { this.escrowStatus = escrowStatus; }
    public String getTransporterId() { return transporterId; }
    public void setTransporterId(String transporterId) { this.transporterId = transporterId; }
}
