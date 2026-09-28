package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ListingItem;

public class HarvestItem {
    private String gradeText;
    private int badgeBgRes;
    private int badgeIconRes;
    private String listedTime;
    private String cropName;
    private String quantity;
    private String location;
    private String unitPrice;
    private String listingId;
    private String farmerId;
    private String farmerName;

    public HarvestItem(String gradeText, int badgeBgRes, int badgeIconRes, String listedTime, 
                       String cropName, String quantity, String location, String unitPrice) {
        this.gradeText = gradeText;
        this.badgeBgRes = badgeBgRes;
        this.badgeIconRes = badgeIconRes;
        this.listedTime = listedTime;
        this.cropName = cropName;
        this.quantity = quantity;
        this.location = location;
        this.unitPrice = unitPrice;
    }

    private HarvestItem(String gradeText, int badgeBgRes, int badgeIconRes, String listedTime,
                        String cropName, String quantity, String location, String unitPrice,
                        String listingId, String farmerId, String farmerName) {
        this(gradeText, badgeBgRes, badgeIconRes, listedTime, cropName, quantity, location, unitPrice);
        this.listingId = listingId;
        this.farmerId = farmerId;
        this.farmerName = farmerName;
    }

    public static HarvestItem fromListing(ListingItem listing) {
        String grade = listing.getGrade();
        if (grade == null || grade.trim().isEmpty()) {
                return new HarvestItem("Verified Harvest", R.drawable.bg_badge_grade_a,
                    R.drawable.ic_check, listing.getCreatedDate(), listing.getCropName(),
                    listing.getQuantity(), listing.getLocation(), listing.getPrice(),
                    listing.getId(), listing.getFarmerId(), listing.getFarmerName());
        }
        int badgeBackground = R.drawable.bg_badge_grade_a;
        if ("B".equalsIgnoreCase(grade)) {
            badgeBackground = R.drawable.bg_badge_grade_b;
        } else if ("C".equalsIgnoreCase(grade)) {
            badgeBackground = R.drawable.bg_badge_grade_c;
        }
        String gradeLabel = "Grade " + grade + " Verified";
        return new HarvestItem(gradeLabel, badgeBackground, R.drawable.ic_check,
                listing.getCreatedDate(), listing.getCropName(), listing.getQuantity(),
            listing.getLocation(), listing.getPrice(), listing.getId(),
            listing.getFarmerId(), listing.getFarmerName());
    }

    public String getGradeText() { return gradeText; }
    public int getBadgeBgRes() { return badgeBgRes; }
    public int getBadgeIconRes() { return badgeIconRes; }
    public String getListedTime() { return listedTime; }
    public String getCropName() { return cropName; }
    public String getQuantity() { return quantity; }
    public String getLocation() { return location; }
    public String getUnitPrice() { return unitPrice; }
    public String getListingId() { return listingId; }
    public String getFarmerId() { return farmerId; }
    public String getFarmerName() { return farmerName; }
}

