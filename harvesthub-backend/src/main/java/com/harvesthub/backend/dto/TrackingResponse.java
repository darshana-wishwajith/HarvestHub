package com.harvesthub.backend.dto;

import lombok.Data;

/** Buyer live tracking (Figma buyer 5.2). Distances in km, ETA in minutes. Position fields are null until the first GPS ping. */
@Data
public class TrackingResponse {
    private String orderId;
    private String status;
    private String cropName;
    private double quantityKg;

    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double dropLatitude;
    private Double dropLongitude;
    private String dropAddress;

    private Double currentLatitude;
    private Double currentLongitude;
    private String lastUpdate;          // ISO local time of the last GPS point
    private int pointCount;

    private double totalKm;
    private Double remainingKm;
    private Double speedKmh;            // from the last two points, null if unknown
    private Integer etaMinutes;
    private int progressPercent;

    private String transporterName;
    private String transporterPhone;
    private String vehicleLabel;
    private String vehiclePlate;
}
