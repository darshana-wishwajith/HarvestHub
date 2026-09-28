package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
