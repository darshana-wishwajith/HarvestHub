package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.TransporterEventRequest;
import com.harvesthub.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {

    @Autowired
    private com.harvesthub.backend.security.CurrentUser currentUser;

    @Autowired
    private com.harvesthub.backend.repository.ListingRepository listingRepository;

    private static final java.util.Set<String> FARMER_STATUSES = java.util.Set.of("HARVESTING", "HARVEST_READY");
    private static final java.util.Set<String> BUYER_STATUSES = java.util.Set.of("LOADING_APPROVED", "LOADING_REJECTED");

    /** Loads the order and verifies the caller is its farmer, buyer, assigned transporter, or staff. */
    private com.harvesthub.backend.model.Order requireParticipant(String orderId) {
        com.harvesthub.backend.model.Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Order not found");
        }
        currentUser.requireParticipantOrStaff(order.getFarmerId(), order.getBuyerId(), order.getTransporterId());
        return order;
    }
    @Autowired
    private OrderService orderService;

    @PostMapping("orders/{orderId}/confirm-deal")
    public ResponseEntity<GenericResponse> confirmDeal(
            @PathVariable String orderId,
            @RequestBody ConfirmDealRequest request) {
        requireParticipant(orderId);
        return ResponseEntity.ok(orderService.confirmDeal(orderId, request));
    }

    @PostMapping("/orders/create")
    public ResponseEntity<?> createOrder(@RequestBody com.harvesthub.backend.model.Order order) {
        currentUser.requireRole("BUYER");
        var me = currentUser.get();
        // Parties, prices of record and workflow state are server-owned.
        order.setOrderId(null);
        order.setBuyerId(me.id());
        order.setBuyerName(me.fullName());
        order.setTransporterId(null);
        order.setEscrowStatus(null);
        order.setStatus(null);
        order.setRating(null);
        order.setRatingComment(null);
        order.setTransportEvents(null);
        if (order.getQuantity() <= 0 || order.getPricePerKg() <= 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Quantity and price must be positive"));
        }
        if (order.getListingId() != null) {
            var listing = listingRepository.findById(order.getListingId()).orElse(null);
            if (listing == null || !"ACTIVE".equals(listing.getStatus())) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Listing is not available"));
            }
            order.setFarmerId(listing.getFarmerId());
            order.setFarmerName(listing.getFarmerName());
            order.setCropName(listing.getCropName());
        } else if (order.getFarmerId() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "listingId or farmerId is required"));
        }
        order.setTotalAmount(order.getQuantity() * order.getPricePerKg());
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<com.harvesthub.backend.model.Order> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody java.util.Map<String, String> request) {
        com.harvesthub.backend.model.Order existing = requireParticipant(orderId);
        String status = request.get("status");
        var me = currentUser.get();
        // Only user-driven milestones may be set directly; money and transport states go through
        // the payment / transporter-event endpoints.
        boolean allowed = me.isStaff()
                || (me.id().equals(existing.getFarmerId()) && FARMER_STATUSES.contains(status))
                || (me.id().equals(existing.getBuyerId()) && BUYER_STATUSES.contains(status));
        if (!allowed) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Status change not permitted");
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, request.get("note")));
    }

    @GetMapping("/orders/farmer/{farmerId}")
    public ResponseEntity<java.util.List<com.harvesthub.backend.model.Order>> getOrdersByFarmer(@PathVariable String farmerId) {
        currentUser.requireSelfOrStaff(farmerId);
        return ResponseEntity.ok(orderService.getOrdersByFarmer(farmerId));
    }

    @GetMapping("/orders/buyer/{buyerId}")
    public ResponseEntity<java.util.List<com.harvesthub.backend.model.Order>> getOrdersByBuyer(@PathVariable String buyerId) {
        currentUser.requireSelfOrStaff(buyerId);
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    @GetMapping("/orders/transporter/{transporterId}")
    public ResponseEntity<java.util.List<com.harvesthub.backend.model.Order>> getOrdersByTransporter(
            @PathVariable String transporterId,
            @RequestParam(required = false) String status) {
        currentUser.requireSelfOrStaff(transporterId);
        return ResponseEntity.ok(orderService.getOrdersByTransporter(transporterId, status));
    }

    @PostMapping("/orders/{orderId}/transporter-events")
    public ResponseEntity<?> applyTransporterEvent(
            @PathVariable String orderId,
            @RequestBody TransporterEventRequest request) {
        request.setTransporterId(currentUser.id()); // the service checks it matches the assigned transporter
        try {
            com.harvesthub.backend.model.Order order = orderService.applyTransporterEvent(orderId, request);
            return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<?> recordAdvancePayment(
            @PathVariable String orderId,
            @RequestBody java.util.Map<String, Object> request) {
        com.harvesthub.backend.model.Order existing = requireParticipant(orderId);
        if (!currentUser.id().equals(existing.getBuyerId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Only the buyer can pay");
        }
        Object amountValue = request.get("amount");
        if (!(amountValue instanceof Number)) {
            return ResponseEntity.badRequest().body("Payment amount is required");
        }
        com.harvesthub.backend.model.Order order = orderService.recordAdvancePayment(
                orderId, ((Number) amountValue).doubleValue());
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<com.harvesthub.backend.model.Order> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(requireParticipant(orderId));
    }

    @PostMapping("/orders/{orderId}/rate")
    public ResponseEntity<com.harvesthub.backend.model.Order> rateOrder(
            @PathVariable String orderId,
            @RequestBody java.util.Map<String, Object> request) {
        com.harvesthub.backend.model.Order existing = requireParticipant(orderId);
        if (!currentUser.id().equals(existing.getBuyerId()) && !currentUser.id().equals(existing.getFarmerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Only the buyer or farmer can rate");
        }
        Integer rating = request.get("rating") instanceof Number n ? n.intValue() : null;
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }
        String comment = (String) request.get("comment");
        return ResponseEntity.ok(orderService.rateOrder(orderId, rating, comment));
    }
}
