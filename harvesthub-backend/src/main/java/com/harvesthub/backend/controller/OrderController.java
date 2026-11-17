package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.TrackingResponse;
import com.harvesthub.backend.dto.TransporterEventRequest;
import com.harvesthub.backend.model.Dispute;
import com.harvesthub.backend.model.LogisticsRecord;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.repository.LogisticsRepository;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.DisputeService;
import com.harvesthub.backend.service.OrderService;
import com.harvesthub.backend.service.TrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Order endpoints. The acting user always comes from the JWT; every state change goes through the
 * {@link OrderService} state machine (IllegalStateException → 400 with {"error": ...}).
 */
@RestController
public class OrderController {

    private final CurrentUser currentUser;
    private final OrderService orderService;
    private final LogisticsRepository logistics;
    private final TrackingService trackingService;
    private final DisputeService disputeService;

    public OrderController(CurrentUser currentUser, OrderService orderService, LogisticsRepository logistics,
                           TrackingService trackingService, DisputeService disputeService) {
        this.trackingService = trackingService;
        this.disputeService = disputeService;
        this.currentUser = currentUser;
        this.orderService = orderService;
        this.logistics = logistics;
    }

    /** Loads the order and verifies the caller is its farmer, buyer, assigned transporter, or staff. */
    private Order requireParticipant(String orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        // drivers an open job request was broadcast to may read it (decision 18 Nov)
        if ("TRANSPORTER_ASSIGNED".equals(order.getStatus()) && order.getOfferedTransporterIds() != null
                && order.getOfferedTransporterIds().contains(currentUser.id())) return order;
        currentUser.requireParticipantOrStaff(order.getFarmerId(), order.getBuyerId(), order.getTransporterId());
        return order;
    }

    // ---- create (workflow 2.1) ----

    /** Scenario A: buyer's purchase request on a verified listing. */
    @PostMapping("/orders/create")
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        currentUser.requireRole("BUYER");
        var me = currentUser.get();
        Order o = new Order();
        o.setBuyerId(me.id());
        o.setBuyerName(me.fullName());
        o.setListingId(order.getListingId());
        o.setQuantity(order.getQuantity());
        o.setPricePerKg(order.getPricePerKg());
        return ResponseEntity.ok(orderService.createOrder(o));
    }

    /** Scenario B: buyer accepts a farmer's supply offer. */
    @PostMapping({"/offers/{offerId}/accept", "/api/offers/{offerId}/accept"})
    public ResponseEntity<Order> acceptOffer(@PathVariable String offerId) {
        currentUser.requireRole("BUYER");
        var me = currentUser.get();
        return ResponseEntity.ok(orderService.acceptSupplyOffer(offerId, me.id(), me.fullName()));
    }

    // ---- negotiation (2.2) ----

    @PostMapping("/orders/{orderId}/confirm-deal")
    public ResponseEntity<GenericResponse> confirmDeal(@PathVariable String orderId, @RequestBody ConfirmDealRequest request) {
        requireParticipant(orderId);
        return ResponseEntity.ok(orderService.confirmDeal(orderId, request, currentUser.id()));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable String orderId) {
        requireParticipant(orderId);
        return ResponseEntity.ok(orderService.cancel(orderId, currentUser.id()));
    }

    // ---- payment (3.2) ----

    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<Order> recordAdvancePayment(@PathVariable String orderId, @RequestBody Map<String, Object> request) {
        requireParticipant(orderId);
        if (!(request.get("amount") instanceof Number amount)) {
            throw new IllegalStateException("Payment amount is required");
        }
        Object method = request.get("method"); // CARD (default) | WALLET
        return ResponseEntity.ok(orderService.recordAdvancePayment(orderId, amount.doubleValue(), currentUser.id(),
                method == null ? "CARD" : method.toString()));
    }

    // ---- farmer / buyer milestones ----

    /** body: {"event": "START_HARVESTING" | "HARVEST_READY" | "APPROVE_LOADING" | "REJECT_LOADING" | "ACCEPT_DELIVERY" | "REJECT_DELIVERY" | "GOODS_RECEIVED", "reason": "..."} */
    @PostMapping("/orders/{orderId}/events")
    public ResponseEntity<Order> partyEvent(@PathVariable String orderId, @RequestBody Map<String, String> body) {
        requireParticipant(orderId);
        return ResponseEntity.ok(orderService.applyPartyEvent(orderId, body.get("event"), currentUser.id(), body.get("reason"),
                body.get("method")));
    }

    /**
     * Older app builds send {"status": "HARVESTING" | "HARVEST_READY" | "LOADING_APPROVED" | "LOADING_REJECTED"}.
     * These are mapped onto the state machine; no other status can be set by a client.
     */
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable String orderId, @RequestBody Map<String, String> request) {
        requireParticipant(orderId);
        String event = switch (String.valueOf(request.get("status"))) {
            case "HARVESTING" -> "START_HARVESTING";
            case "HARVEST_READY" -> "HARVEST_READY";
            case "LOADING_APPROVED" -> "APPROVE_LOADING";
            case "LOADING_REJECTED" -> "REJECT_LOADING";
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Status change not permitted");
        };
        return ResponseEntity.ok(orderService.applyPartyEvent(orderId, event, currentUser.id(), request.get("note")));
    }

    @PostMapping("/orders/{orderId}/transporter-events")
    public ResponseEntity<Order> applyTransporterEvent(@PathVariable String orderId, @RequestBody TransporterEventRequest request) {
        currentUser.requireRole("TRANSPORTER");
        request.setTransporterId(currentUser.id()); // the service checks it matches the assigned transporter
        return ResponseEntity.ok(orderService.applyTransporterEvent(orderId, request));
    }

    // ---- queries ----

    @GetMapping("/orders/farmer/{farmerId}")
    public ResponseEntity<List<Order>> getOrdersByFarmer(@PathVariable String farmerId) {
        currentUser.requireSelfOrStaff(farmerId);
        return ResponseEntity.ok(orderService.getOrdersByFarmer(farmerId));
    }

    @GetMapping("/orders/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getOrdersByBuyer(@PathVariable String buyerId) {
        currentUser.requireSelfOrStaff(buyerId);
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    @GetMapping("/orders/transporter/{transporterId}")
    public ResponseEntity<List<Order>> getOrdersByTransporter(@PathVariable String transporterId,
                                                              @RequestParam(required = false) String status) {
        currentUser.requireSelfOrStaff(transporterId);
        return ResponseEntity.ok(orderService.getOrdersByTransporter(transporterId, status));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(requireParticipant(orderId));
    }

    /** Vehicle, distance, fee breakdown and live tracking points (proposal §6.8). */
    @GetMapping("/orders/{orderId}/logistics")
    public ResponseEntity<LogisticsRecord> getLogistics(@PathVariable String orderId) {
        requireParticipant(orderId);
        return logistics.findByOrderId(orderId).map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No logistics plan yet"));
    }

    /** Live tracking for the buyer (Figma buyer 5.2): last GPS point, remaining distance, ETA. */
    @GetMapping("/orders/{orderId}/tracking")
    public ResponseEntity<TrackingResponse> tracking(@PathVariable String orderId) {
        requireParticipant(orderId);
        return ResponseEntity.ok(trackingService.tracking(orderId));
    }

    /** Figma buyer 6.2 "Report Issue with Handover": body {"reason": "...", "details": "..."} */
    @PostMapping("/orders/{orderId}/disputes")
    public ResponseEntity<Dispute> openDispute(@PathVariable String orderId, @RequestBody Map<String, Object> body) {
        requireParticipant(orderId);
        List<String> photos = body.get("photoUrls") instanceof List<?> l ? l.stream().map(String::valueOf).toList() : null;
        return ResponseEntity.ok(disputeService.open(orderId, currentUser.id(),
                body.get("reason") instanceof String r ? r : null, body.get("details") instanceof String s ? s : null, photos));
    }

    @GetMapping("/orders/{orderId}/disputes")
    public ResponseEntity<List<Dispute>> disputes(@PathVariable String orderId) {
        requireParticipant(orderId);
        return ResponseEntity.ok(disputeService.forOrder(orderId));
    }

    // ---- rating (5.4) ----

    /** body: {"rating": 1-5, "comment": "...", "toRole": "BUYER" | "FARMER" | "TRANSPORTER"} */
    @PostMapping("/orders/{orderId}/rate")
    public ResponseEntity<GenericResponse> rateOrder(@PathVariable String orderId, @RequestBody Map<String, Object> request) {
        Order o = requireParticipant(orderId);
        int stars = request.get("rating") instanceof Number n ? n.intValue() : 0;
        String toRole = (String) request.get("toRole");
        if (toRole == null) { // old clients: buyer rates farmer, farmer rates buyer
            toRole = currentUser.id().equals(o.getBuyerId()) ? "FARMER" : "BUYER";
        }
        orderService.rate(orderId, currentUser.id(), toRole, stars, (String) request.get("comment"));
        return ResponseEntity.ok(new GenericResponse(true, "Thank you for your feedback"));
    }
}
