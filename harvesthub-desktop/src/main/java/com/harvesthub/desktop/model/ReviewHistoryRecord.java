package com.harvesthub.desktop.model;

import com.harvesthub.desktop.api.ReviewHistoryRecordDto;

/**
 * Row model for the Review History table.
 */
public class ReviewHistoryRecord {

    public enum DecisionKind {
        GRADE_A,
        GRADE_B,
        GRADE_C,
        RETAKE,
        REJECTED
    }

    private final String id;
    private final String avatarCode;
    private final String cropName;
    private final String variety;
    private final String farmerName;
    private final String location;
    private final String decisionLabel;
    private final DecisionKind decisionKind;
    private final String dateReviewed;

    public ReviewHistoryRecord(String id, String avatarCode, String cropName, String variety,
                               String farmerName, String location,
                               String decisionLabel, DecisionKind decisionKind,
                               String dateReviewed) {
        this.id = id;
        this.avatarCode = avatarCode;
        this.cropName = cropName;
        this.variety = variety;
        this.farmerName = farmerName;
        this.location = location;
        this.decisionLabel = decisionLabel;
        this.decisionKind = decisionKind;
        this.dateReviewed = dateReviewed;
    }

    public ReviewHistoryRecord(String avatarCode, String cropName, String variety,
                               String farmerName, String location,
                               String decisionLabel, DecisionKind decisionKind,
                               String dateReviewed) {
        this(null, avatarCode, cropName, variety, farmerName, location, decisionLabel, decisionKind, dateReviewed);
    }

    public static ReviewHistoryRecord fromDto(ReviewHistoryRecordDto dto) {
        DecisionKind kind = DecisionKind.GRADE_A;
        try {
            if (dto.decisionKind != null) kind = DecisionKind.valueOf(dto.decisionKind.toUpperCase());
        } catch (Exception ignored) {}
        return new ReviewHistoryRecord(
                dto.id,
                dto.avatarCode != null ? dto.avatarCode : "CR",
                dto.cropName,
                dto.variety != null ? dto.variety : "Standard",
                dto.farmerName,
                dto.location,
                dto.decisionLabel != null ? dto.decisionLabel : "Approved",
                kind,
                dto.dateReviewed != null ? dto.dateReviewed : "Recently"
        );
    }

    public String getId() {
        return id;
    }

    public String getAvatarCode() {
        return avatarCode;
    }

    public String getCropName() {
        return cropName;
    }

    public String getVariety() {
        return variety;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getLocation() {
        return location;
    }

    public String getDecisionLabel() {
        return decisionLabel;
    }

    public DecisionKind getDecisionKind() {
        return decisionKind;
    }

    public String getDateReviewed() {
        return dateReviewed;
    }
}
