package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors GET /api/dashboard/activity row shape — same fields as the existing ActivityLog model. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityLogDto {
    public Long id;
    public String cropName;
    public String variety;
    public String farmerName;
    public String location;
    public String grade;
    public String status;
    public String timestamp;
}
