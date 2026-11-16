package com.harvesthub.mobile.model;

/** Crop catalogue entry (backend Crop). verified == false means added by a farmer, pending officer review. */
public class CropInfo {
    private String id;
    private String name;
    private String category;
    private Double standardPrice;
    private Boolean verified;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Double getStandardPrice() { return standardPrice; }
    public boolean isPending() { return Boolean.FALSE.equals(verified); }

    @Override
    public String toString() {
        return name; // shown by the dropdown adapter
    }
}
