package com.harvesthub.backend.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Money and logistics rules (proposal §6.8, §6.12; workflow 3.1–5.3; user decisions 14 Nov 2026).
 * Defaults live here; any value can be overridden with harvesthub.rules.* properties.
 */
@Data
@ConfigurationProperties(prefix = "harvesthub.rules")
public class MarketplaceRules {

    /** Buyer convenience fee on (crop value + transport fee). */
    private double buyerFeeRate = 0.03;
    /** Platform commission deducted from the transport fee. */
    private double transporterCommissionRate = 0.05;
    /** Share of the crop value paid in advance (released to the farmer at Start Journey). */
    private double advanceCropRate = 0.20;
    /** Handling charge per kg (Weight × HandlingFee). */
    private double handlingFeePerKg = 2.0;
    /** Straight-line distance is multiplied by this when Google Maps is unavailable. */
    private double roadDistanceFactor = 1.3;
    /** A listing may only be created this close (metres) to the registered farm (workflow 1.2). */
    private double farmRadiusMeters = 500;
    /** Buyer must approve the pre-loading photos within this time, else they are auto-approved (decision 15 Nov). */
    private int loadingApprovalMinutes = 15;
    /** Speed used for the ETA when the transporter has sent fewer than two GPS points. */
    private double defaultTruckSpeedKmh = 40;
    /** A job request sent to a driver expires after this time and goes to the next driver (decision 15 Nov). */
    private int transportRequestMinutes = 10;
    /** A job request goes to this many nearest matching free drivers at once; the first to accept gets it (decision 18 Nov). */
    private int dispatchBroadcastSize = 5;
    /** Pre-loading photos the driver must upload before buyer approval (decision 15 Nov: 2..4). */
    private int minLoadingPhotos = 2;
    private int maxLoadingPhotos = 4;

    private List<Vehicle> vehicles = new ArrayList<>(List.of(
            new Vehicle("THREE_WHEELER", "Three-wheeler", 500, 80),
            new Vehicle("MINI_LORRY", "Mini lorry (1 ton)", 1_000, 120),
            new Vehicle("LORRY", "Lorry (4 ton)", 4_000, 200),
            new Vehicle("TRUCK", "Truck (10 ton)", 10_000, 350)));

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vehicle {
        private String type;
        private String label;
        private double capacityKg;
        private double ratePerKm;
    }
}
