package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class CropListing {
    @SerializedName("id")
    private String id;
    
    @SerializedName("cropName")
    private String cropName;
    
    @SerializedName("price")
    private double price;
    
    @SerializedName("quantity")
    private int quantity;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
