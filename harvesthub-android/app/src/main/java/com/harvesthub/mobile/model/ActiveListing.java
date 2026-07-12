package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ActiveListing {
    @SerializedName("listingId")
    private String listingId;

    @SerializedName("listingCode")
    private String listingCode;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("status")
    private String status; // DRAFT, PENDING, UNDER REVIEW, LISTED, ONGOING, DEACTIVATED

    @SerializedName("estYieldKg")
    private String estYieldKg;

    @SerializedName("expectedPricePerKg")
    private String expectedPricePerKg;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("updatedTime")
    private String updatedTime;

    @SerializedName("location")
    private String location;

    public ActiveListing(String listingId, String listingCode, String cropName, String thumbnailUrl, String status, String estYieldKg, String expectedPricePerKg, String createdDate, String updatedTime, String location) {
        this.listingId = listingId;
        this.listingCode = listingCode;
        this.cropName = cropName;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.estYieldKg = estYieldKg;
        this.expectedPricePerKg = expectedPricePerKg;
        this.createdDate = createdDate;
        this.updatedTime = updatedTime;
        this.location = location;
    }

    public String getListingId() { return listingId; }
    public String getListingCode() { return listingCode; }
    public String getCropName() { return cropName; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getStatus() { return status; }
    public String getEstYieldKg() { return estYieldKg; }
    public String getExpectedPricePerKg() { return expectedPricePerKg; }
    public String getCreatedDate() { return createdDate; }
    public String getUpdatedTime() { return updatedTime; }
    public String getLocation() { return location; }

    public void setStatus(String status) { this.status = status; }
}
