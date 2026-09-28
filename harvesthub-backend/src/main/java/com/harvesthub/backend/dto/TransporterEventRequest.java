package com.harvesthub.backend.dto;

import lombok.Data;

@Data
public class TransporterEventRequest {
    private String transporterId;
    private String eventType;
    private String note;
    private Double latitude;
    private Double longitude;
    private Integer photoCount;
    private Boolean buyerVerified;
    private String rejectionReason;
}