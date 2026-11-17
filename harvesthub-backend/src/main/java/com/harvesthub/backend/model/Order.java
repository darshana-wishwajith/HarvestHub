package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Central transaction record (proposal §6.5, §6.9). Status values follow the workflow; see
 * {@link com.harvesthub.backend.service.OrderWorkflowService} for the allowed transitions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    @Id
    private String orderId;
    private String farmerId;
    private String buyerId;
    private String listingId;
    private double quantity;
    private double pricePerKg;
    private String deliveryDate;
    private String status;
    private String createdDate;

    private double totalAmount;
    private String buyerName;
    private String cropName;
    private String farmerName;
    private String transporterId;
    private String escrowStatus;
    private java.util.List<StatusChange> statusHistory;
    private Integer rating;
    private String ratingComment;
    private String pickupDate;
    private String actualDeliveryDate;
    private java.util.List<TransportEvent> transportEvents;

    // ---- Origin (workflow 2.1) ----
    /** PURCHASE_REQUEST (buyer → listing) or SUPPLY_OFFER (farmer → wanted ad) */
    private String origin;
    private String supplyOfferId;
    private String wantedAdId;

    // ---- Negotiation (workflow 2.2): both parties confirm the same terms ----
    private boolean farmerConfirmed;
    private boolean buyerConfirmed;

    // ---- Money (workflow 3.2, 4.3, 5.2, 5.3) — all LKR ----
    private Payment payment;

    // ---- Logistics snapshot for the apps (full record in "logistics") ----
    private String vehicleType;
    private String vehicleLabel;
    private Double distanceKm;
    private String transporterName;
    private String transporterPhone;
    private String vehiclePlate;
    private String pickupAddress;
    private String dropAddress;

    // ---- 2-way delivery confirmation (workflow 5.2) ----
    private boolean transporterConfirmedDelivery;
    private boolean buyerConfirmedReceipt;

    /** Users who already rated someone on this order (fromUserId list). */
    private java.util.List<String> ratedBy;

    // buyer flow (Figma buyer 4.2, 5.1, 6.2 — 15 Nov)
    private String advancePaymentMethod;          // CARD | WALLET
    private String balancePaymentMethod;          // CARD | WALLET
    private java.util.List<String> loadingEvidenceUrls;
    private String loadingEvidenceAt;             // yyyy-MM-dd HH:mm:ss
    private Long loadingApprovalDeadline;         // epoch ms; auto-approve after this (15 min)
    private boolean loadingAutoApproved;
    private boolean disputeOpen;
    /** Epoch ms when the pending job request to the assigned driver expires (decision 15 Nov: 10 min). */
    private Long transportRequestExpiresAt;
    /** Drivers the open job request was broadcast to (decision 18 Nov: nearest matching drivers, first to accept wins). */
    private java.util.List<String> offeredTransporterIds = new java.util.ArrayList<>();
    /** Driver brought rejected goods back to the farm (decision 15 Nov). */
    private String goodsReturnedAt;

    public double getTotalPrice() {
        return totalAmount > 0 ? totalAmount : (quantity * pricePerKg);
    }

    public void setTotalPrice(double totalPrice) {
        this.totalAmount = totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private double cropValue;          // quantity × agreed price
        private double transportFee;       // distance × rate + weight × handling
        private double buyerFee;           // 3% of (crop + transport)
        private double advanceCropPortion; // 20% of crop value
        private double advanceDue;         // advanceCropPortion + transportFee + buyerFee
        private double balanceDue;         // remaining 80% of crop value
        private boolean advancePaid;
        private boolean balancePaid;
        private boolean farmerAdvanceReleased;
        private boolean settled;
        private double emptyTripFee;
        private double transporterPayout;  // 95% of the transport fee (what the driver earns on completion)
        private String settlementNote;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransportEvent {
        private String eventType;
        private String timestamp;
        private String transporterId;
        private String note;
        private Double latitude;
        private Double longitude;
        private Integer photoCount;
        private Boolean buyerVerified;
        private String rejectionReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChange {
        private String status;
        private String timestamp;
        private String note;
    }
}
