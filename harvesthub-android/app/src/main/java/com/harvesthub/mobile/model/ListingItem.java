package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ListingItem {
    public enum Status {
        DRAFT, PENDING, UNDER_REVIEW, LISTED, ONGOING, DEACTIVATED, WANTED, COMPLETED, CANCELLED, ACTIVE, HISTORY
    }

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

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("updatedTime")
    private String updatedTime;

    @SerializedName("status")
    private Status status;

    private int imageResource;

    public ListingItem(String id, String cropName, String location, String quantity, String price, String createdDate, String updatedTime, Status status) {
        this(id, cropName, location, quantity, price, createdDate, updatedTime, status, 0);
    }

    public ListingItem(String id, String cropName, String location, String quantity, String price, String createdDate, String updatedTime, Status status, int imageResource) {
        this.id = id;
        this.cropName = cropName;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
        this.createdDate = createdDate;
        this.updatedTime = updatedTime;
        this.status = status;
        this.imageResource = imageResource;
    }

    public ListingItem(String id, String cropName, String location, String quantity, String price, String createdDate, boolean isUrgent, String type) {
        this.id = id;
        this.cropName = cropName;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
        this.createdDate = createdDate;
        this.updatedTime = isUrgent ? "URGENT" : "NORMAL";
        try {
            this.status = Status.valueOf(type);
        } catch (Exception e) {
            this.status = Status.PENDING;
        }
        this.imageResource = 0;
    }

    public String getId() { return id; }
    public String getCropName() { return cropName; }
    public String getLocation() { return location; }
    public String getQuantity() { return quantity; }
    public String getPrice() { return price; }
    public String getCreatedDate() { return createdDate; }
    public String getUpdatedTime() { return updatedTime; }
    public Status getStatus() { return status; }
    public int getImageResource() { return imageResource; }
    
    // For compatibility with previous code
    public String getType() { return status.name(); }
    public boolean isUrgent() { return false; }
}
