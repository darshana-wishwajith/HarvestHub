package com.example.project_part_2;

public class HarvestItem {
    private String gradeText;
    private int badgeBgRes;
    private int badgeIconRes;
    private String listedTime;
    private String cropName;
    private String quantity;
    private String location;
    private String unitPrice;

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

    public String getGradeText() { return gradeText; }
    public int getBadgeBgRes() { return badgeBgRes; }
    public int getBadgeIconRes() { return badgeIconRes; }
    public String getListedTime() { return listedTime; }
    public String getCropName() { return cropName; }
    public String getQuantity() { return quantity; }
    public String getLocation() { return location; }
    public String getUnitPrice() { return unitPrice; }
}
