package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDetailDto {
    private String id;
    private String cropName;
    private String farmerName;
    private String farmerAvatar;
    private String farmerIdNumber;
    private String cooperativeName;
    private String location;
    private String submittedTimeAgo;
    private String submittedTimestamp;
    private String gpsCoordinates;
    private String deviceModel;
    private String referenceCode;
    private String quantity;
    private String variancePercent;
    private String pricePerUnit;
    private String totalValuation;
    private String status;
    private int mediaFileCount;
    private String mediaCaption1;
    private String mediaCaption2;
    private String videoLabel;
}
