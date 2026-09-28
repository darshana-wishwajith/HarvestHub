package com.harvesthub.desktop.model;

/**
 * Row model for a listing awaiting review on the Verification Queue screen.
 */
public class QueueListing {

    public enum CropTone {
        AMBER, BROWN, TEAL, PURPLE
    }

    private final String cropName;
    private final String farmerName;
    private final String location;
    private final String submittedAgo;
    private final String quantity;
    private final String pricePerKg;
    private final String statusLabel;
    private final boolean claimable;
    private final CropTone tone;
    private final String iconLiteral;

    public QueueListing(String cropName, String farmerName, String location,
                        String submittedAgo, String quantity, String pricePerKg,
                        String statusLabel, boolean claimable,
                        CropTone tone, String iconLiteral) {
        this.cropName = cropName;
        this.farmerName = farmerName;
        this.location = location;
        this.submittedAgo = submittedAgo;
        this.quantity = quantity;
        this.pricePerKg = pricePerKg;
        this.statusLabel = statusLabel;
        this.claimable = claimable;
        this.tone = tone;
        this.iconLiteral = iconLiteral;
    }

    public String getCropName() {
        return cropName;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getLocation() {
        return location;
    }

    public String getSubmittedAgo() {
        return submittedAgo;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPricePerKg() {
        return pricePerKg;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public boolean isClaimable() {
        return claimable;
    }

    public CropTone getTone() {
        return tone;
    }

    public String getIconLiteral() {
        return iconLiteral;
    }

    public boolean isPending() {
        return "Pending".equalsIgnoreCase(statusLabel);
    }
}
