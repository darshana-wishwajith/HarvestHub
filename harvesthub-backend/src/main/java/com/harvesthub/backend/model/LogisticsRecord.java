package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/** "Logistics &amp; Tracking" collection: vehicle, distance, fee and route points per order. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "logistics")
public class LogisticsRecord {
    @Id
    private String id;
    @Indexed(unique = true)
    private String orderId;

    private double weightKg;
    private String vehicleType;
    private String vehicleLabel;
    private double vehicleCapacityKg;
    private double ratePerKm;
    private double handlingFeePerKg;

    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double dropLatitude;
    private Double dropLongitude;
    private double distanceKm;
    /** GOOGLE_MAPS or ESTIMATE (straight line × road factor) */
    private String distanceSource;

    private double distanceCost;     // Distance × Rate
    private double handlingCost;     // Weight × HandlingFee
    private double transportFee;     // distanceCost + handlingCost

    private String transporterId;
    private List<String> declinedTransporterIds = new ArrayList<>();
    private List<TrackPoint> trackingPoints = new ArrayList<>();
    private String createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackPoint {
        private double latitude;
        private double longitude;
        private String timestamp;
    }
}
