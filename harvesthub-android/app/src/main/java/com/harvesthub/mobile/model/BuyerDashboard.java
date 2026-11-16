package com.harvesthub.mobile.model;

import java.util.List;

/** GET /dashboard/buyer/{id} (Figma buyer Home 564:330). Field names match the backend DTO. */
public class BuyerDashboard {
    public String buyerName;
    public String greeting;
    public boolean hasDeliveryLocation;
    public String deliveryAddress;
    public int activeOrders;
    public double escrowHeld;
    public int pendingOffers;
    public int offersExpiringToday;
    public int openWantedAds;
    public int actionRequired;
    public double walletBalance;
    public int trustPercent;
    public Double ratingAverage;
    public int ratingCount;
    public List<Highlight> highlights;
    public List<EscrowOrder> escrowOrders;

    public static class Highlight {
        public String listingId;
        public String cropName;
        public String grade;
        public double pricePerKg;
        public double quantityKg;
        public String location;
        public String farmerName;
        public String imageUrl;
        public String listedAt;
    }

    public static class EscrowOrder {
        public String orderId;
        public String cropName;
        public String farmerName;
        public String status;
        public double amountHeld;
    }
}
