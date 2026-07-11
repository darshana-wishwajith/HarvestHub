package com.harvesthub.mobile.ui.farmer;

import com.google.gson.annotations.SerializedName;

public class LocationRequest {
    @SerializedName("farmerId")
    private String farmerId;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    public LocationRequest(String farmerId, double latitude, double longitude) {
        this.farmerId = farmerId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getFarmerId() { return farmerId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
