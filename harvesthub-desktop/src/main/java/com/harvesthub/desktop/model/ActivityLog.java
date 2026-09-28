package com.harvesthub.desktop.model;

/**
 * Row model for the Recent Activity Logs table on the Officer Portal dashboard.
 */
public class ActivityLog {

    private final String cropName;
    private final String variety;
    private final String farmerName;
    private final String location;
    private final String grade;
    private final String status;
    private final String timestamp;

    public ActivityLog(String cropName, String variety, String farmerName,
                       String location, String grade, String status, String timestamp) {
        this.cropName = cropName;
        this.variety = variety;
        this.farmerName = farmerName;
        this.location = location;
        this.grade = grade;
        this.status = status;
        this.timestamp = timestamp;
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

    public String getGrade() {
        return grade;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
