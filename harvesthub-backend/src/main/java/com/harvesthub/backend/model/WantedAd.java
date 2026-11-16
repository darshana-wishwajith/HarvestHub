package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wanted_ads")
public class WantedAd {
    @Id
    private String requestId;
    private String cropName;
    private String location;
    private double requiredQuantityKg;
    private double targetPricePerKg;
    @com.fasterxml.jackson.annotation.JsonProperty("isUrgent")
    private boolean isUrgent;
    private String buyerId;
    private String createdDate;
    private String deadline;
    private String targetGrade;
    private String status;            // OPEN | CLOSED
    private String buyerName;
    private String notes;             // "Additional details" (Figma buyer 2.3)
    private String closedDate;
    private String closedReason;      // OFFER_ACCEPTED | DEADLINE | MANUAL
    private String republishedFrom;

    // computed per request, not stored (Figma buyer 2.4 "3 Supply Offers Received")
    @org.springframework.data.annotation.Transient
    private int offerCount;
    @org.springframework.data.annotation.Transient
    private int pendingOfferCount;
}

