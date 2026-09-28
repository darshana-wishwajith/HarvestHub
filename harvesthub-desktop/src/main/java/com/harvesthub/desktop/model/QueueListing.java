package com.harvesthub.desktop.model;

import com.harvesthub.desktop.api.QueueListingDto;

/**
 * Row model for a listing awaiting review on the Verification Queue screen.
 */
public class QueueListing {

    public enum CropTone {
        AMBER, BROWN, TEAL, PURPLE
    }

    private final String id;
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

    public QueueListing(String id, String cropName, String farmerName, String location,
                        String submittedAgo, String quantity, String pricePerKg,
                        String statusLabel, boolean claimable,
                        CropTone tone, String iconLiteral) {
        this.id = id;
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

    public QueueListing(String cropName, String farmerName, String location,
                        String submittedAgo, String quantity, String pricePerKg,
                        String statusLabel, boolean claimable,
                        CropTone tone, String iconLiteral) {
        this("LIST-" + Math.abs(farmerName.hashCode() % 9000 + 1000), cropName, farmerName, location, submittedAgo, quantity, pricePerKg, statusLabel, claimable, tone, iconLiteral);
    }

    public static QueueListing fromDto(QueueListingDto dto) {
        CropTone tone = CropTone.AMBER;
        try {
            if (dto.tone != null) tone = CropTone.valueOf(dto.tone.toUpperCase());
        } catch (Exception ignored) {}
        return new QueueListing(
                dto.id,
                dto.cropName,
                dto.farmerName,
                dto.location,
                dto.submittedAgo != null ? dto.submittedAgo : "Recently",
                dto.quantity,
                dto.pricePerKg,
                dto.statusLabel != null ? dto.statusLabel : "Pending",
                dto.claimable,
                tone,
                dto.iconLiteral != null ? dto.iconLiteral : "fas-seedling"
        );
    }

    public String getId() {
        return id;
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
