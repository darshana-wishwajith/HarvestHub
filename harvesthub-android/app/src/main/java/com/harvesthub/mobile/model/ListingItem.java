package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ListingItem {
    @SerializedName("id")
    private String id;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("location")
    private String location;

    @SerializedName("quantity")
    private String quantity;

    @SerializedName("price")
    private String price;

    @SerializedName("status")
    private String status; // e.g., "Active", "Completed", "Cancelled"

    @SerializedName("isUrgent")
    private boolean isUrgent;

    @SerializedName("type")
    private String type; // "WANTED", "ACTIVE", "HISTORY"

    public ListingItem(String id, String cropName, String location, String quantity, String price, String status, boolean isUrgent, String type) {
        this.id = id;
        this.cropName = cropName;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.isUrgent = isUrgent;
        this.type = type;
    }

    public String getId() { return id; }
    public String getCropName() { return cropName; }
    public String getLocation() { return location; }
    public String getQuantity() { return quantity; }
    public String getPrice() { return price; }
    public String getStatus() { return status; }
    public boolean isUrgent() { return isUrgent; }
    public String getType() { return type; }
}
