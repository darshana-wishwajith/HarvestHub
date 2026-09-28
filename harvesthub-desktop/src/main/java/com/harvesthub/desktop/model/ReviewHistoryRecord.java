package com.harvesthub.desktop.model;

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

    private final String avatarCode;
    private final String cropName;
    private final String variety;
    private final String farmerName;
    private final String location;
    private final String decisionLabel;
    private final DecisionKind decisionKind;
    private final String dateReviewed;

    public ReviewHistoryRecord(String avatarCode, String cropName, String variety,
                               String farmerName, String location,
                               String decisionLabel, DecisionKind decisionKind,
                               String dateReviewed) {
        this.avatarCode = avatarCode;
        this.cropName = cropName;
        this.variety = variety;
        this.farmerName = farmerName;
        this.location = location;
        this.decisionLabel = decisionLabel;
        this.decisionKind = decisionKind;
        this.dateReviewed = dateReviewed;
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
