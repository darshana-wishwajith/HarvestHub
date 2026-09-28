package com.harvesthub.desktop.model;

import com.harvesthub.desktop.api.ActivityLogDto;

/**
 * Row model for the Recent Activity Logs table on the Officer Portal dashboard.
 */
public class ActivityLog {

    private final String id;
    private final String cropName;
    private final String variety;
    private final String farmerName;
    private final String location;
    private final String grade;
    private final String status;
    private final String timestamp;

    public ActivityLog(String id, String cropName, String variety, String farmerName,
                       String location, String grade, String status, String timestamp) {
        this.id = id;
        this.cropName = cropName;
        this.variety = variety;
        this.farmerName = farmerName;
        this.location = location;
        this.grade = grade;
        this.status = status;
        this.timestamp = timestamp;
    }

    public ActivityLog(String cropName, String variety, String farmerName,
                       String location, String grade, String status, String timestamp) {
        this(null, cropName, variety, farmerName, location, grade, status, timestamp);
    }

    public static ActivityLog fromDto(ActivityLogDto dto) {
        return new ActivityLog(
                dto.id,
                dto.cropName,
                dto.variety != null ? dto.variety : "Standard",
                dto.farmerName,
                dto.location,
                dto.grade != null ? dto.grade : "A",
                dto.status != null ? dto.status : "Pending",
                dto.timestamp != null ? dto.timestamp : "Today"
        );
    }

    public String getId() {
        return id;
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
