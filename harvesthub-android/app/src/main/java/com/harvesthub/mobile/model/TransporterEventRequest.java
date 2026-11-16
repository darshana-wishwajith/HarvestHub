package com.harvesthub.mobile.model;

public class TransporterEventRequest {
    private String transporterId;
    private String eventType;
    private String note;
    private Integer photoCount;
    private Boolean buyerVerified;
    private String rejectionReason;

    public TransporterEventRequest(String transporterId, String eventType, String note) {
        this.transporterId = transporterId;
        this.eventType = eventType;
        this.note = note;
    }

    public void setPhotoCount(Integer photoCount) { this.photoCount = photoCount; }
    public void setBuyerVerified(Boolean buyerVerified) { this.buyerVerified = buyerVerified; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    // GPS + uploaded evidence (backend TransporterEventRequest, 15 Nov)
    private Double latitude;
    private Double longitude;
    private java.util.List<String> photoUrls;

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setPhotoUrls(java.util.List<String> photoUrls) { this.photoUrls = photoUrls; }
}