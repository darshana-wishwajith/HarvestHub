package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class ListingItem {
    public enum Status {
        @SerializedName(value = "DRAFT", alternate = {"draft", "Draft", "DRAFTING", "Drafting"})
        DRAFT,
        @SerializedName(value = "PENDING", alternate = {"pending", "Pending"})
        PENDING,
        @SerializedName(value = "UNDER_REVIEW", alternate = {"under_review", "Under Review", "UNDER REVIEW", "under review"})
        UNDER_REVIEW,
        @SerializedName(value = "LISTED", alternate = {"listed", "Listed"})
        LISTED,
        @SerializedName(value = "ONGOING", alternate = {"ongoing", "Ongoing"})
        ONGOING,
        @SerializedName(value = "DEACTIVATED", alternate = {"deactivated", "Deactivated"})
        DEACTIVATED,
        @SerializedName(value = "WANTED", alternate = {"wanted", "Wanted"})
        WANTED,
        @SerializedName(value = "COMPLETED", alternate = {"completed", "Completed"})
        COMPLETED,
        @SerializedName(value = "CANCELLED", alternate = {"cancelled", "Cancelled", "CANCELED"})
        CANCELLED,
        @SerializedName(value = "ACTIVE", alternate = {"active", "Active"})
        ACTIVE,
        @SerializedName(value = "APPROVED", alternate = {"approved", "Approved"})
        APPROVED,
        @SerializedName(value = "RETAKE_REQUESTED", alternate = {"retake_requested", "Retake Requested", "RETAKE"})
        RETAKE_REQUESTED,
        @SerializedName(value = "REJECTED", alternate = {"rejected", "Rejected"})
        REJECTED,
        @SerializedName(value = "HISTORY", alternate = {"history", "History"})
        HISTORY
    }

    @SerializedName("id")
    private String id;

    @SerializedName("farmerId")
    private String farmerId;

    @SerializedName("farmerName")
    private String farmerName;

    @SerializedName("cropName")
    private String cropName;

    @SerializedName("location")
    private String location;

    @SerializedName("quantity")
    private String quantity;

    @SerializedName("price")
    private String price;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("updatedTime")
    private String updatedTime;

    @SerializedName("status")
    private Status status;

    @SerializedName("grade")
    private String grade;

    private int imageResource;

    public ListingItem(String id, String cropName, String location, String quantity, String price, String createdDate, String updatedTime, Status status) {
        this(id, cropName, location, quantity, price, createdDate, updatedTime, status, 0);
    }

    public ListingItem(String id, String cropName, String location, String quantity, String price, String createdDate, String updatedTime, Status status, int imageResource) {
        this.id = id;
        this.cropName = cropName;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
        this.createdDate = createdDate;
        this.updatedTime = updatedTime;
        this.status = status;
        this.imageResource = imageResource;
    }

    public ListingItem(String id, String cropName, String location, String quantity, String price, String createdDate, boolean isUrgent, String type) {
        this.id = id;
        this.cropName = cropName;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
        this.createdDate = createdDate;
        this.updatedTime = isUrgent ? "URGENT" : "NORMAL";
        try {
            this.status = Status.valueOf(type);
        } catch (Exception e) {
            this.status = Status.PENDING;
        }
        this.imageResource = 0;
    }

    public String getId() { return id; }
    public String getFarmerId() { return farmerId; }
    public String getFarmerName() { return farmerName; }
    public String getCropName() { return cropName; }
    public String getLocation() { return location; }
    public String getQuantity() { return quantity; }
    public String getPrice() { return price; }
    public String getCreatedDate() { return createdDate; }
    public String getUpdatedTime() { return updatedTime; }
    public Status getStatus() { return status != null ? status : Status.PENDING; }
    public String getGrade() { return grade; }
    public int getImageResource() { return imageResource; }

    // For compatibility with previous code
    public String getType() { return status.name(); }

    // from the backend (14 Nov): order link and officer feedback
    @SerializedName("activeOrderId")
    private String activeOrderId;
    @SerializedName("reviewNotes")
    private String reviewNotes;
    @SerializedName("referenceCode")
    private String referenceCode;
    @SerializedName("cropPending")
    private boolean cropPending;
    /** Open purchase request still being negotiated (the listing stays on the market until a deal is accepted). */
    @SerializedName("negotiationOrderId")
    private String negotiationOrderId;
    @SerializedName("negotiationBuyerName")
    private String negotiationBuyerName;
    @SerializedName("negotiationCount")
    private int negotiationCount;

    public String getNegotiationOrderId() { return negotiationOrderId; }
    public String getNegotiationBuyerName() { return negotiationBuyerName; }
    public int getNegotiationCount() { return negotiationCount; }

    /** The order the card's Chat / Track buttons belong to: the running order, else the open negotiation. */
    public String getChatOrderId() { return activeOrderId != null ? activeOrderId : negotiationOrderId; }

    /** [wide photo, close-up photo, video] as "/uploads/..." paths; the card shows the wide photo. */
    @SerializedName("mediaUrls")
    private java.util.List<String> mediaUrls;

    public java.util.List<String> getMediaUrls() { return mediaUrls; }

    /** Small server-made JPEG of an uploaded photo: "/uploads/x.jpg" -> "/uploads/thumb/x.jpg?size=N". */
    public static String thumbUrl(String upload, int size) {
        if (upload == null || !upload.startsWith("/uploads/") || upload.toLowerCase().endsWith(".mp4")) return upload;
        return "/uploads/thumb/" + upload.substring("/uploads/".length()) + "?size=" + size;
    }

    /** First photo of the capture, or null. */
    public String getCoverPhotoUrl() {
        if (mediaUrls == null) return null;
        for (String u : mediaUrls) if (u != null && !u.toLowerCase().endsWith(".mp4")) return u;
        return null;
    }
    private boolean urgent;

    public String getActiveOrderId() { return activeOrderId; }
    public String getReviewNotes() { return reviewNotes; }
    public String getReferenceCode() { return referenceCode; }
    public boolean isCropPending() { return cropPending; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
}
