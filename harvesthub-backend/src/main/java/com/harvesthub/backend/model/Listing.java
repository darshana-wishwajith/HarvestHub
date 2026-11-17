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
    /**
     * Workflow 1.2–1.5: DRAFT → PENDING (Pending Verification) → UNDER_REVIEW → ACTIVE (Listed)
     * or RETAKE_REQUESTED / REJECTED (Action Required). ONGOING while an order is running, COMPLETED after.
     * quantity / price hold plain numbers as text ("1200", "250").
     */
    private String status;
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
    /** Every officer decision on this listing, oldest first (approve / retake / reject); desktop review history. */
    private java.util.List<ReviewEntry> reviews;

    // Field verification media and telemetry
    private String gpsCoordinates;
    private String deviceModel;
    private String referenceCode;
    private String variancePercent;
    private Integer mediaFileCount;
    private String mediaCaption1;
    private String mediaCaption2;
    private String videoLabel;

    // Location-locked, geotagged capture (workflow 1.2–1.3)
    private Double gpsLatitude;
    private Double gpsLongitude;
    private Double distanceFromFarmMeters;
    /** /uploads/... links: [wide-angle photo, close-up photo, 10 s video] */
    private java.util.List<String> mediaUrls;
    private String capturedAt;
    /** Crop was typed in via "Add New" and is not yet in the verified catalogue. */
    private Boolean cropPending;
    /** Order currently running on this listing (status ONGOING). */
    private String activeOrderId;
    /** Status before the farmer deactivated the listing (ACTIVE or PENDING); restored on re-activate. */
    private String previousStatus;

    public double quantityKgValue() {
        return parseNumber(quantity);
    }

    public double pricePerKgValue() {
        return parseNumber(price);
    }

    public static double parseNumber(String s) {
        if (s == null) return 0;
        String digits = s.replaceAll("[^0-9.]", "");
        try {
            return digits.isEmpty() ? 0 : Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReviewEntry {
        private String decision;      // APPROVED | RETAKE | REJECTED
        private String grade;         // A/B/C when approved
        private String notes;
        private String officerId;
        private String officerName;
        private String at;            // ISO local date-time
    }
}
