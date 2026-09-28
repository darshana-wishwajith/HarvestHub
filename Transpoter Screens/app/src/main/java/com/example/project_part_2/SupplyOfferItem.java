package com.example.project_part_2;

public class SupplyOfferItem {
    private String id;
    private String gradeText;
    private int gradeBadgeBgRes;
    private boolean hasCheckmark;
    private String offerTime;
    private String farmerName;
    private String location;
    private String estimatedQty;
    private String offeredPrice;

    public SupplyOfferItem(String id, String gradeText, int gradeBadgeBgRes, boolean hasCheckmark,
                           String offerTime, String farmerName, String location, String estimatedQty, String offeredPrice) {
        this.id = id;
        this.gradeText = gradeText;
        this.gradeBadgeBgRes = gradeBadgeBgRes;
        this.hasCheckmark = hasCheckmark;
        this.offerTime = offerTime;
        this.farmerName = farmerName;
        this.location = location;
        this.estimatedQty = estimatedQty;
        this.offeredPrice = offeredPrice;
    }

    public String getId() { return id; }
    public String getGradeText() { return gradeText; }
    public int getGradeBadgeBgRes() { return gradeBadgeBgRes; }
    public boolean isHasCheckmark() { return hasCheckmark; }
    public String getOfferTime() { return offerTime; }
    public String getFarmerName() { return farmerName; }
    public String getLocation() { return location; }
    public String getEstimatedQty() { return estimatedQty; }
    public String getOfferedPrice() { return offeredPrice; }
}
