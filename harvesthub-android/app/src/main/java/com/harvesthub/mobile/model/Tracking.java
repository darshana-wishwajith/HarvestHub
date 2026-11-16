package com.harvesthub.mobile.model;

/** GET /orders/{id}/tracking (Figma buyer 5.2). Position fields are null until the driver's first GPS ping. */
public class Tracking {
    public String orderId;
    public String status;
    public String cropName;
    public double quantityKg;
    public Double pickupLatitude;
    public Double pickupLongitude;
    public String pickupAddress;
    public Double dropLatitude;
    public Double dropLongitude;
    public String dropAddress;
    public Double currentLatitude;
    public Double currentLongitude;
    public String lastUpdate;
    public int pointCount;
    public double totalKm;
    public Double remainingKm;
    public Double speedKmh;
    public Integer etaMinutes;
    public int progressPercent;
    public String transporterName;
    public String transporterPhone;
    public String vehicleLabel;
    public String vehiclePlate;
}
