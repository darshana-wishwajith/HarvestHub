package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "listings")
public class Listing {
    @Id
    private String id;
    private String farmerId;
    private String farmerName;
    private String farmerAvatar;
    private String farmerIdNumber;
    private String cooperativeName;
    private String cropName;
    private String variety;
    private String location;
    private String quantity;
    private String price;
    private String createdDate;
    private String updatedTime;
    private String status; // DRAFT, PENDING, UNDER_REVIEW, ACTIVE, APPROVED, RETAKE_REQUESTED, REJECTED, DELIVERED
    private String buyerName;
    private String quantityKg;
    private String totalValue;
    private String finalPricePerKg;

    // Quality and review fields
    private String grade; // "A", "B", "C"
    private String reviewNotes;
    private String claimedByOfficerId;
    private String claimedByOfficerName;
    private String reviewedByOfficerId;
    private String reviewedByOfficerName;
    private String reviewedAt;

    // Field verification media and telemetry
    private String gpsCoordinates;
    private String deviceModel;
    private String referenceCode;
    private String variancePercent;
    private Integer mediaFileCount;
    private String mediaCaption1;
    private String mediaCaption2;
    private String videoLabel;
}
