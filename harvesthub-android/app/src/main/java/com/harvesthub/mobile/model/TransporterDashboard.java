package com.harvesthub.mobile.model;

import java.util.ArrayList;
import java.util.List;

/** GET /dashboard/transporter/{id} – Figma transporter Home, Jobs and History headers. Amounts in LKR. */
public class TransporterDashboard {
    public String fullName;
    public boolean online;
    public boolean busy;
    public boolean suspended;
    public double amountOwed;

    public String vehicleType;
    public String vehicleLabel;
    public String vehiclePlate;
    public double capacityKg;
    public String vehicleBodyType;
    public boolean licenceUploaded;
    public String baseAddress;

    public double balance;
    public double earningsToday;
    public double earningsWeek;
    public double earningsTotal;
    public double grossFreightTotal;
    public double commissionTotal;
    public double emptyTripTotal;
    public double damageChargesTotal;

    public int tripsCompleted;
    public int emptyTrips;
    public int rejectedDeliveries;
    public int tripsThisWeek;
    public Double ratingAverage;
    public Integer ratingCount;

    public List<Job> pendingRequests = new ArrayList<>();
    public Job activeJob;
    public List<Payout> recentPayouts = new ArrayList<>();

    public static class Job {
        public String orderId;
        public String status;
        public String cropName;
        public double quantityKg;
        public String pickupAddress;
        public String dropoffAddress;
        public String farmerName;
        public String buyerName;
        public double distanceKm;
        public double transportFee;
        public double payout;
        public Long expiresAt;
        public String vehicleLabel;
    }

    public static class Payout {
        public String title;
        public String type;
        public double amount;
        public String timestamp;
        public String orderId;
    }
}
