package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class TransportJob {
    @SerializedName("id")
    private String id;
    
    @SerializedName("pickupLocation")
    private String pickupLocation;
    
    @SerializedName("destination")
    private String destination;
    
    @SerializedName("status")
    private String status;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
