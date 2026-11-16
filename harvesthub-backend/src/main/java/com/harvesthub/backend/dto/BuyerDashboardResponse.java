package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Buyer Home (Figma buyer "Buyer Home Dashboard" 564:330) — computed only from stored data. */
@Data
public class BuyerDashboardResponse {
    private String buyerName;
    private String greeting;
    private boolean hasDeliveryLocation;
    private String deliveryAddress;

    private int activeOrders;
    private double escrowHeld;              // buyer money currently locked in escrow
    private int pendingOffers;              // PENDING supply offers on the buyer's open wanted ads
    private int offersExpiringToday;        // ... whose wanted ad deadline is today
    private int openWantedAds;
    private int actionRequired;             // orders waiting for the buyer (pay, approve, inspect, confirm)

    private double walletBalance;
    private int trustPercent;
    private Double ratingAverage;
    private int ratingCount;

    private List<Highlight> highlights = new ArrayList<>();
    private List<EscrowOrder> escrowOrders = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Highlight {
        private String listingId;
        private String cropName;
        private String grade;
        private double pricePerKg;
        private double quantityKg;
        private String location;
        private String farmerName;
        private String imageUrl;
        private String listedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscrowOrder {
        private String orderId;
        private String cropName;
        private String farmerName;
        private String status;
        private double amountHeld;
    }
}
