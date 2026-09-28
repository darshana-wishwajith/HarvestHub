package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors GET /api/listings/{id} — same fields as the existing Listing model. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListingDetailDto {
    public Long id;
    public String cropName;
    public String farmerName;
    public String farmerAvatar;
    public String farmerIdNumber;
    public String cooperativeName;
    public String location;
    public String submittedTimeAgo;
    public String submittedTimestamp;
    public String gpsCoordinates;
    public String deviceModel;
    public String referenceCode;
    public String quantity;
    public String variancePercent;
    public String pricePerUnit;
    public String totalValuation;
    public String status;
    public int mediaFileCount;
    public String mediaCaption1;
    public String mediaCaption2;
    public String videoLabel;
}
