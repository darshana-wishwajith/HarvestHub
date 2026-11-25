package com.harvesthub.backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Transporter Home + History header: availability, vehicle, wallet, earnings, jobs. All amounts in LKR. */
@Data
public class TransporterDashboardResponse {
    private String fullName;
    private boolean online;          // driver's switch
    private boolean busy;            // has an active job
    private boolean suspended;       // negative wallet
    private double amountOwed;       // > 0 when suspended

    private String vehicleType;
    private String vehicleLabel;
    private String vehiclePlate;
    private double capacityKg;
    private String vehicleBodyType;
    private boolean licenceUploaded;
    private String baseAddress;

    private double balance;
    private double earningsToday;
    private double earningsWeek;
    private double earningsTotal;    // delivery payouts + empty trip fees received
    private double grossFreightTotal; // transport fees of completed trips
    private double commissionTotal;  // 5% platform commission on those trips
    private double emptyTripTotal;
    private double damageChargesTotal;

    private int tripsCompleted;
    private int emptyTrips;
    private int rejectedDeliveries;
    private int tripsThisWeek;
    private Double ratingAverage;
    private Integer ratingCount;

    private List<Job> pendingRequests = new ArrayList<>();
    private Job activeJob;
    private List<Payout> recentPayouts = new ArrayList<>();

    @Data
    public static class Job {
        private String orderId;
        private String status;
        private String cropName;
        private double quantityKg;
        private String pickupAddress;
        private String dropoffAddress;
        private String pickupDate;
        private String farmerName;
        private String buyerName;
        private double distanceKm;
        private double transportFee;
        private double payout;       // 95% of the transport fee
        private Long expiresAt;      // epoch ms, pending requests only
        private String vehicleLabel;
    }

    @Data
    public static class Payout {
        private String title;
        private String type;         // TRANSPORT_PAYOUT | EMPTY_TRIP_FEE | DAMAGE_CHARGE | DEPOSIT | WITHDRAWAL
        private double amount;       // signed
        private String timestamp;
        private String orderId;
    }
}
