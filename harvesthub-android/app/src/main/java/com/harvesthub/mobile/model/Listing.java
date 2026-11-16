package com.harvesthub.mobile.model;

public class Listing {
    private String id;
    private String farmerId;
    private String cropName;
    private String location;
    private String quantity;
    private String price;
    private String status;

    public Listing() {
    }

    public Listing(String farmerId, String cropName, String location, String quantity, String price, String status) {
        this.farmerId = farmerId;
        this.cropName = cropName;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Location-locked, geotagged capture (workflow 1.2-1.3); sent on create / retake
    private Double gpsLatitude;
    private Double gpsLongitude;
    private java.util.List<String> mediaUrls;
    private String deviceModel;
    private String capturedAt;
    private String variety;
    private String grade;
    private String referenceCode;
    private String reviewNotes;

    public Double getGpsLatitude() { return gpsLatitude; }
    public void setGpsLatitude(Double v) { gpsLatitude = v; }
    public Double getGpsLongitude() { return gpsLongitude; }
    public void setGpsLongitude(Double v) { gpsLongitude = v; }
    public java.util.List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(java.util.List<String> v) { mediaUrls = v; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String v) { deviceModel = v; }
    public String getCapturedAt() { return capturedAt; }
    public void setCapturedAt(String v) { capturedAt = v; }
    public String getVariety() { return variety; }
    public void setVariety(String v) { variety = v; }
    public String getGrade() { return grade; }
    public String getReferenceCode() { return referenceCode; }
    public String getReviewNotes() { return reviewNotes; }
}
