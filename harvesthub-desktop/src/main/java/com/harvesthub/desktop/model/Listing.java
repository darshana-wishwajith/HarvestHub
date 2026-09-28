package com.harvesthub.desktop.model;

/**
 * Detailed crop listing model passed to the Review screen.
 */
public class Listing {

    private String id;
    private String cropName;
    private String farmerName;
    private String farmerAvatar;
    private String farmerIdNumber;
    private String cooperativeName;
    private String location;
    private String submittedTimeAgo;
    private String submittedTimestamp;
    private String gpsCoordinates;
    private String deviceModel;
    private String referenceCode;
    private String quantity;
    private String variancePercent;
    private String pricePerUnit;
    private String totalValuation;
    private String status;
    private int mediaFileCount;
    private String mediaCaption1;
    private String mediaCaption2;
    private String videoLabel;

    public Listing() {
    }

    public Listing(String id, String cropName, String farmerName, String farmerAvatar,
                   String farmerIdNumber, String cooperativeName, String location,
                   String submittedTimeAgo, String submittedTimestamp, String gpsCoordinates,
                   String deviceModel, String referenceCode, String quantity,
                   String variancePercent, String pricePerUnit, String totalValuation,
                   String status, int mediaFileCount, String mediaCaption1,
                   String mediaCaption2, String videoLabel) {
        this.id = id;
        this.cropName = cropName;
        this.farmerName = farmerName;
        this.farmerAvatar = farmerAvatar;
        this.farmerIdNumber = farmerIdNumber;
        this.cooperativeName = cooperativeName;
        this.location = location;
        this.submittedTimeAgo = submittedTimeAgo;
        this.submittedTimestamp = submittedTimestamp;
        this.gpsCoordinates = gpsCoordinates;
        this.deviceModel = deviceModel;
        this.referenceCode = referenceCode;
        this.quantity = quantity;
        this.variancePercent = variancePercent;
        this.pricePerUnit = pricePerUnit;
        this.totalValuation = totalValuation;
        this.status = status;
        this.mediaFileCount = mediaFileCount;
        this.mediaCaption1 = mediaCaption1;
        this.mediaCaption2 = mediaCaption2;
        this.videoLabel = videoLabel;
    }

    /**
     * Returns a default sample Listing (Paddy / Kalinga Perera) for fallback/direct Review Control view.
     */
    public static Listing getDefaultSample() {
        return fromQueueListing(new QueueListing(
                "Paddy", "Kalinga Perera", "Anuradhapura North",
                "2 hours ago", "450 kg", "LKR 120/kg",
                "Pending", true, QueueListing.CropTone.AMBER, "fas-seedling"
        ));
    }

    public static Listing fromDto(com.harvesthub.desktop.api.ListingDetailDto dto) {
        String avatar = dto.farmerAvatar != null ? dto.farmerAvatar : extractInitials(dto.farmerName);
        return new Listing(
                dto.id,
                dto.cropName,
                dto.farmerName,
                avatar,
                dto.farmerIdNumber != null ? dto.farmerIdNumber : "ID: 902182",
                dto.cooperativeName != null ? dto.cooperativeName : "HarvestHub Central",
                dto.location,
                dto.submittedTimeAgo != null ? dto.submittedTimeAgo : "Recently",
                dto.submittedTimestamp != null ? dto.submittedTimestamp : "Oct 24, 2023, 10:32 AM",
                dto.gpsCoordinates != null ? dto.gpsCoordinates : "7.8731° N, 80.7718° E",
                dto.deviceModel != null ? dto.deviceModel : "Android CropScanner v4.2",
                dto.referenceCode != null ? dto.referenceCode : dto.id,
                dto.quantity,
                dto.variancePercent != null ? dto.variancePercent : "0.0% dev",
                dto.pricePerUnit,
                dto.totalValuation != null ? dto.totalValuation : "Rs. 189,000",
                dto.status != null ? dto.status : "Pending",
                dto.mediaFileCount > 0 ? dto.mediaFileCount : 3,
                dto.mediaCaption1 != null ? dto.mediaCaption1 : "Wide Angle - Field View",
                dto.mediaCaption2 != null ? dto.mediaCaption2 : "Close-Up - Sample Quality",
                dto.videoLabel != null ? dto.videoLabel : "360° Field & Sample Video (10s)"
        );
    }

    /**
     * Creates a detailed Listing from a QueueListing object.
     */
    public static Listing fromQueueListing(QueueListing q) {
        String avatar = extractInitials(q.getFarmerName());
        String ref = q.getId() != null ? q.getId() : ("LIST-" + Math.abs(q.getFarmerName().hashCode() % 9000 + 1000));
        return new Listing(
                ref,
                q.getCropName(),
                q.getFarmerName(),
                avatar,
                "ID: 902182",
                "HarvestHub Central",
                q.getLocation(),
                q.getSubmittedAgo(),
                "Oct 24, 2023, 10:32 AM",
                "7.8731° N, 80.7718° E",
                "Android CropScanner v4.2",
                ref,
                q.getQuantity(),
                "-1.1% dev",
                q.getPricePerKg(),
                calculateValuation(q.getQuantity(), q.getPricePerKg()),
                q.getStatusLabel(),
                3,
                "Wide Angle - Field View",
                "Close-Up - Sample Quality",
                "360° Field & Sample Video (10s)"
        );
    }

    /**
     * Creates a detailed Listing from a ReviewHistoryRecord object.
     */
    public static Listing fromReviewHistoryRecord(ReviewHistoryRecord r) {
        String ref = "HIST-" + Math.abs(r.getFarmerName().hashCode() % 9000 + 1000);
        return new Listing(
                ref,
                r.getCropName(),
                r.getFarmerName(),
                r.getAvatarCode(),
                "ID: 884190",
                "HarvestHub District",
                r.getLocation(),
                "Reviewed on " + r.getDateReviewed(),
                r.getDateReviewed() + ", 02:15 PM",
                "6.9271° N, 79.8612° E",
                "iOS CropScanner v3.1",
                ref,
                "50 Bag",
                "0.0% dev",
                "LKR 3,800",
                "LKR 190,000",
                r.getDecisionLabel(),
                3,
                "Overview - Farm Plot",
                "Close-Up - Harvest Batch",
                "360° Field Inspection Video (15s)"
        );
    }

    private static String extractInitials(String name) {
        if (name == null || name.isBlank()) return "KP";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static String calculateValuation(String qty, String price) {
        try {
            double qVal = Double.parseDouble(qty.replaceAll("[^0-9.]", ""));
            double pVal = Double.parseDouble(price.replaceAll("[^0-9.]", ""));
            double total = qVal * pVal;
            return String.format("LKR %,.0f", total);
        } catch (Exception e) {
            return "LKR 189,000";
        }
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getFarmerAvatar() { return farmerAvatar; }
    public void setFarmerAvatar(String farmerAvatar) { this.farmerAvatar = farmerAvatar; }

    public String getFarmerIdNumber() { return farmerIdNumber; }
    public void setFarmerIdNumber(String farmerIdNumber) { this.farmerIdNumber = farmerIdNumber; }

    public String getCooperativeName() { return cooperativeName; }
    public void setCooperativeName(String cooperativeName) { this.cooperativeName = cooperativeName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSubmittedTimeAgo() { return submittedTimeAgo; }
    public void setSubmittedTimeAgo(String submittedTimeAgo) { this.submittedTimeAgo = submittedTimeAgo; }

    public String getSubmittedTimestamp() { return submittedTimestamp; }
    public void setSubmittedTimestamp(String submittedTimestamp) { this.submittedTimestamp = submittedTimestamp; }

    public String getGpsCoordinates() { return gpsCoordinates; }
    public void setGpsCoordinates(String gpsCoordinates) { this.gpsCoordinates = gpsCoordinates; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getVariancePercent() { return variancePercent; }
    public void setVariancePercent(String variancePercent) { this.variancePercent = variancePercent; }

    public String getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(String pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public String getTotalValuation() { return totalValuation; }
    public void setTotalValuation(String totalValuation) { this.totalValuation = totalValuation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getMediaFileCount() { return mediaFileCount; }
    public void setMediaFileCount(int mediaFileCount) { this.mediaFileCount = mediaFileCount; }

    public String getMediaCaption1() { return mediaCaption1; }
    public void setMediaCaption1(String mediaCaption1) { this.mediaCaption1 = mediaCaption1; }

    public String getMediaCaption2() { return mediaCaption2; }
    public void setMediaCaption2(String mediaCaption2) { this.mediaCaption2 = mediaCaption2; }

    public String getVideoLabel() { return videoLabel; }
    public void setVideoLabel(String videoLabel) { this.videoLabel = videoLabel; }
}
