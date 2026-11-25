package com.harvesthub.mobile.model;

import com.google.gson.annotations.SerializedName;

public class Order {
    @SerializedName("orderId")
    private String id;
    @SerializedName("farmerId")
    private String farmerId;
    @SerializedName("buyerId")
    private String buyerId;
    @SerializedName("listingId")
    private String listingId;
    @SerializedName("quantity")
    private double quantity;
    @SerializedName("pricePerKg")
    private double pricePerKg;
    @SerializedName("deliveryDate")
    private String deliveryDate;
    private String status;
    @SerializedName("createdDate")
    private String createdAt;
    private double totalAmount;
    private String cropName;
    private String farmerName;
    private String escrowStatus;
    private String transporterId;

    public Order() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    private String cropType;
    private double cropTotal;
    private String transporterName;
    private String vehicleTier;
    private double transportCost;

    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    public double getCropTotal() { return cropTotal; }
    public void setCropTotal(double cropTotal) { this.cropTotal = cropTotal; }
    public String getTransporterName() { return transporterName; }
    public void setTransporterName(String transporterName) { this.transporterName = transporterName; }
    public String getVehicleTier() { return vehicleTier; }
    public void setVehicleTier(String vehicleTier) { this.vehicleTier = vehicleTier; }
    public double getTransportCost() { return transportCost; }
    public void setTransportCost(double transportCost) { this.transportCost = transportCost; }
    public String getEscrowStatus() { return escrowStatus; }
    public void setEscrowStatus(String escrowStatus) { this.escrowStatus = escrowStatus; }
    public String getTransporterId() { return transporterId; }

    /** Drivers an open job request was broadcast to. */
    private java.util.List<String> offeredTransporterIds;

    /** True when this driver owns the job, or the open request was sent to them. */
    public boolean isForDriver(String userId) {
        if (userId == null) return false;
        if (userId.equals(transporterId)) return true;
        return "TRANSPORTER_ASSIGNED".equals(getStatus()) && offeredTransporterIds != null && offeredTransporterIds.contains(userId);
    }

    public int offeredCount() {
        return offeredTransporterIds == null ? 0 : offeredTransporterIds.size();
    }
    public void setTransporterId(String transporterId) { this.transporterId = transporterId; }

    // ---- fields from the backend order workflow (OrderService) ----
    private String buyerName;
    private String pickupDate;
    private String actualDeliveryDate;
    private String origin;
    private boolean farmerConfirmed;
    private boolean buyerConfirmed;
    private Payment payment;
    private String vehicleType;
    private String vehicleLabel;
    private Double distanceKm;
    private String transporterPhone;
    private String vehiclePlate;
    private String pickupAddress;
    private String dropAddress;
    private java.util.List<String> ratedBy;
    private java.util.List<StatusChange> statusHistory;
    // buyer flow
    private String advancePaymentMethod;
    private String balancePaymentMethod;
    private java.util.List<String> loadingEvidenceUrls;
    private String loadingEvidenceAt;
    private Long loadingApprovalDeadline;   // epoch ms
    private boolean loadingAutoApproved;
    private boolean disputeOpen;
    private boolean transporterConfirmedDelivery;
    private boolean buyerConfirmedReceipt;
    private String supplyOfferId;
    private String wantedAdId;
    private java.util.List<TransportEvent> transportEvents;
    // transporter flow
    private Long transportRequestExpiresAt;   // epoch ms, pending job request
    private String goodsReturnedAt;
    public Long getTransportRequestExpiresAt() { return transportRequestExpiresAt; }
    public String getGoodsReturnedAt() { return goodsReturnedAt; }

    /** What the driver earns for this trip: 95% of the transport fee. */
    public double driverPayout() {
        if (payment == null) return 0;
        return payment.transporterPayout > 0 ? payment.transporterPayout : Math.round(payment.transportFee * 95) / 100.0;
    }

    public String getAdvancePaymentMethod() { return advancePaymentMethod; }
    public String getBalancePaymentMethod() { return balancePaymentMethod; }
    public java.util.List<String> getLoadingEvidenceUrls() { return loadingEvidenceUrls; }
    public String getLoadingEvidenceAt() { return loadingEvidenceAt; }
    public Long getLoadingApprovalDeadline() { return loadingApprovalDeadline; }
    public boolean isLoadingAutoApproved() { return loadingAutoApproved; }
    public boolean isDisputeOpen() { return disputeOpen; }
    public boolean isTransporterConfirmedDelivery() { return transporterConfirmedDelivery; }
    public boolean isBuyerConfirmedReceipt() { return buyerConfirmedReceipt; }
    public String getSupplyOfferId() { return supplyOfferId; }
    public String getWantedAdId() { return wantedAdId; }
    public java.util.List<TransportEvent> getTransportEvents() { return transportEvents; }

    /** Last transporter event of a type (e.g. LOADING_EVIDENCE_SUBMITTED) or null. */
    public TransportEvent lastEvent(String type) {
        if (transportEvents == null) return null;
        for (int i = transportEvents.size() - 1; i >= 0; i--) {
            if (type.equals(transportEvents.get(i).eventType)) return transportEvents.get(i);
        }
        return null;
    }

    /** When a status was reached (ISO timestamp) or null. */
    public String reachedAt(String status) {
        if (statusHistory == null) return null;
        for (int i = statusHistory.size() - 1; i >= 0; i--) {
            if (status.equals(statusHistory.get(i).status)) return statusHistory.get(i).timestamp;
        }
        return null;
    }

    public String getBuyerName() { return buyerName; }
    public String getPickupDate() { return pickupDate; }
    public String getActualDeliveryDate() { return actualDeliveryDate; }
    public String getOrigin() { return origin; }
    public boolean isFarmerConfirmed() { return farmerConfirmed; }
    public boolean isBuyerConfirmed() { return buyerConfirmed; }
    public Payment getPayment() { return payment; }
    public String getVehicleType() { return vehicleType; }
    public String getVehicleLabel() { return vehicleLabel; }
    public Double getDistanceKm() { return distanceKm; }
    public String getTransporterPhone() { return transporterPhone; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getPickupAddress() { return pickupAddress; }
    public String getDropAddress() { return dropAddress; }
    public java.util.List<String> getRatedBy() { return ratedBy; }
    public java.util.List<StatusChange> getStatusHistory() { return statusHistory; }

    /** Crop value = agreed quantity × agreed price. */
    public double cropValue() {
        return payment != null && payment.cropValue > 0 ? payment.cropValue : quantity * pricePerKg;
    }

    /** Short id for screens: last 6 characters, upper case. */
    public String shortId() {
        if (id == null) return "";
        return (id.length() > 6 ? id.substring(id.length() - 6) : id).toUpperCase(java.util.Locale.US);
    }

    public static class Payment {
        public double cropValue;
        public double transportFee;
        public double buyerFee;
        public double advanceCropPortion;
        public double advanceDue;
        public double balanceDue;
        public boolean advancePaid;
        public boolean balancePaid;
        public boolean farmerAdvanceReleased;
        public boolean settled;
        public double emptyTripFee;
        public double transporterPayout;
        public String settlementNote;
    }

    public static class TransportEvent {
        public String eventType;
        public String timestamp;
        public String note;
        public Double latitude;
        public Double longitude;
        public Integer photoCount;
    }

    public static class StatusChange {
        public String status;
        public String timestamp;
        public String note;
    }
}
