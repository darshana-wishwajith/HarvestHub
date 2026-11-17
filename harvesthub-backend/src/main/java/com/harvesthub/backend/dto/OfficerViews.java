package com.harvesthub.backend.dto;

import com.harvesthub.backend.model.Order;

import java.util.List;

/**
 * Response shapes for the Agricultural Officer desktop portal (decision 15 Nov: only four roles,
 * officers also monitor users, orders and disputes). Values the database does not have stay null –
 * the portal shows "Not recorded" instead of invented numbers.
 */
public final class OfficerViews {

    private OfficerViews() {
    }

    public record Stats(
            long pendingCount,          // waiting for an officer (pending + under review)
            long unclaimedCount,
            long inReviewByMe,
            long reviewedTodayCount,    // decisions by any officer today
            long myReviewedToday,
            long myReviewedTotal,
            int approvalRate,           // my approvals / my decisions, %
            long openDisputes,
            long activeOrders,
            long problemOrders,
            long totalUsers,
            long suspendedUsers,
            long unreadNotifications) {
    }

    public record Activity(String listingId, String cropName, String farmerName, String location,
                           String kind,       // SUBMITTED | CLAIMED | APPROVED | RETAKE | REJECTED
                           String grade, String by, String at) {
    }

    public record QueueItem(String id, String cropName, String variety, String farmerId, String farmerName,
                            String location, Double quantityKg, Double pricePerKg, String submittedAt,
                            String status, String statusLabel, String claimedByOfficerId,
                            String claimedByOfficerName, boolean claimedByMe, int mediaCount,
                            Double distanceFromFarmMeters, boolean cropPending, boolean resubmitted,
                            String coverUrl) {
    }

    public record Check(String label, boolean ok, String detail) {
    }

    public record UserBrief(String id, String fullName, String role, String email, String mobile, String address,
                            Integer trustScore, Double ratingAverage, Integer ratingCount, Integer completedOrders,
                            boolean accountSuspended) {
    }

    public record ListingDetail(String id, String referenceCode, String cropName, String variety, boolean cropPending,
                                String status, String statusLabel, Double quantityKg, Double pricePerKg,
                                Double totalValue, String submittedAt, String capturedAt, List<String> mediaUrls,
                                Double gpsLatitude, Double gpsLongitude, Double distanceFromFarmMeters,
                                String deviceModel, Double farmLatitude, Double farmLongitude, String farmAddress,
                                String claimedByOfficerId, String claimedByOfficerName, boolean claimedByMe,
                                String grade, String reviewNotes, String reviewedByOfficerName, String reviewedAt,
                                UserBrief farmer, List<Check> checks, List<HistoryItem> reviews) {
    }

    public record HistoryItem(String listingId, String cropName, String variety, String farmerName, String location,
                              String decision, String grade, String notes, String officerId, String officerName,
                              boolean byMe, String at, String listingStatus, String coverUrl) {
    }

    public record UserRow(String id, String fullName, String role, String email, String mobile, String address,
                          Integer trustScore, Double ratingAverage, Integer completedOrders,
                          boolean accountSuspended, String suspensionReason, Double walletBalance,
                          boolean walletSuspended, String vehicleType, String vehiclePlate) {
    }

    public record ListingBrief(String id, String cropName, String status, Double quantityKg, Double pricePerKg,
                               String grade, String createdDate) {
    }

    public record OrderRow(String orderId, String cropName, Double quantityKg, String farmerId, String farmerName,
                           String buyerId, String buyerName, String transporterId, String transporterName,
                           String status, String stage, Double cropValue, Double totalAmount, String createdDate,
                           boolean disputeOpen) {
    }

    public record UserDetail(UserRow user, String nic, Double latitude, Double longitude, Double vehicleCapacityKg,
                             String vehicleBodyType, String licenceUrl, String suspendedAt, String suspendedByName,
                             String reactivatedAt, Integer failedOrders, Double walletPenalties,
                             List<ListingBrief> listings, List<OrderRow> orders, List<DisputeRow> disputes) {
    }

    public record DisputeRow(String id, String type, String status, String reason, String details,
                             String raisedByUserId, String raisedByName, String raisedByRole, String createdAt,
                             String resolvedAt, String resolutionNote, String resolvedByName,
                             List<String> photoUrls, String orderId, String cropName, String orderStatus,
                             String orderStatusAtOpen) {
    }

    public record OrderDetail(OrderRow order, Double pricePerKg, String deliveryDate, String pickupAddress,
                              String dropAddress, Double distanceKm, String vehicleLabel, String vehiclePlate,
                              Order.Payment payment, String advancePaymentMethod, String balancePaymentMethod,
                              List<Order.StatusChange> statusHistory, List<Order.TransportEvent> transportEvents,
                              List<String> loadingEvidenceUrls, String loadingEvidenceAt, Integer rating,
                              String ratingComment, UserBrief farmer, UserBrief buyer, UserBrief transporter,
                              List<DisputeRow> disputes) {
    }

    public record DisputeDetail(DisputeRow dispute, OrderRow order, Order.Payment payment, UserBrief raisedBy,
                                UserBrief farmer, UserBrief buyer, UserBrief transporter,
                                List<DisputeRow> otherDisputes) {
    }
}
