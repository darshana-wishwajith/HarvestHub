package com.harvesthub.backend.service;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.dto.ConfirmDealRequest;
import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.TransporterEventRequest;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Order state machine and liability engine.
 *
 * <pre>
 * NEGOTIATING ──both confirm──▶ AWAITING_PAYMENT ──advance paid──▶ DISPATCHING ─▶ TRANSPORTER_ASSIGNED | PAUSED
 * TRANSPORTER_ASSIGNED ──ACCEPT──▶ TRANSPORTER_ACCEPTED ──farmer──▶ HARVESTING ──farmer──▶ HARVEST_READY
 * HARVEST_READY ──ARRIVED_PICKUP──▶ ARRIVED_AT_PICKUP ──evidence──▶ AWAITING_BUYER_LOADING_APPROVAL
 *   ├─buyer approves──▶ LOADING_APPROVED ──START_JOURNEY (farmer gets 20%)──▶ IN_TRANSIT ──▶ ARRIVED_AT_DESTINATION
 *   └─buyer rejects──▶ PRELOAD_REJECTED (buyer 100% refund, farmer pays Empty Trip Fee to transporter)
 * ARRIVED_AT_DESTINATION ├─buyer accepts + pays 80%──▶ BALANCE_PAID ──driver "Delivery Completed" + buyer "Goods Received"──▶ COMPLETED
 *                        └─rejected──▶ RETURN_JOURNEY (buyer 100% refund, transporter charged the crop advance)
 * NEGOTIATING / AWAITING_PAYMENT ──cancel──▶ CANCELLED
 * </pre>
 */
@Service
public class OrderService {

    public static final Set<String> FINAL = Set.of("COMPLETED", "PRELOAD_REJECTED", "RETURN_JOURNEY", "RETURNED", "CANCELLED");
    static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final OrderRepository orderRepository;
    private final ListingRepository listings;
    private final UserRepository users;
    private final LogisticsRepository logistics;
    private final SupplyOfferRepository offers;
    private final WantedAdRepository wantedAds;
    private final LedgerService ledger;
    private final DispatchService dispatch;
    private final NotificationService notifications;
    private final TrustService trust;
    private final DistanceService distance;
    private final MarketplaceRules rules;
    private final RatingRepository ratings;

    public OrderService(OrderRepository orderRepository, ListingRepository listings, UserRepository users,
                        LogisticsRepository logistics, SupplyOfferRepository offers, WantedAdRepository wantedAds,
                        LedgerService ledger, DispatchService dispatch, NotificationService notifications,
                        TrustService trust, DistanceService distance, MarketplaceRules rules,
                        RatingRepository ratings) {
        this.orderRepository = orderRepository;
        this.listings = listings;
        this.users = users;
        this.logistics = logistics;
        this.offers = offers;
        this.wantedAds = wantedAds;
        this.ledger = ledger;
        this.dispatch = dispatch;
        this.notifications = notifications;
        this.trust = trust;
        this.distance = distance;
        this.rules = rules;
        this.ratings = ratings;
    }

    // ------------------------------------------------------------------ helpers

    static String now() {
        return LocalDateTime.now().format(ISO);
    }

    static void setStatus(Order o, String status, String note) {
        o.setStatus(status);
        if (o.getStatusHistory() == null) o.setStatusHistory(new ArrayList<>());
        o.getStatusHistory().add(new Order.StatusChange(status, now(), note));
    }

    private Order require(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found"));
    }

    private static void expect(Order o, String... allowed) {
        for (String s : allowed) if (s.equals(o.getStatus())) return;
        throw new IllegalStateException("This step is not possible while the order is " + o.getStatus()
                + " (expected " + String.join(" or ", allowed) + ")");
    }

    private Order.Payment pay(Order o) {
        if (o.getPayment() == null) throw new IllegalStateException("Order has no payment quote yet");
        return o.getPayment();
    }

    // ------------------------------------------------------------------ queries

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public List<Order> getOrdersByFarmer(String farmerId) {
        return sorted(orderRepository.findByFarmerId(farmerId));
    }

    public List<Order> getOrdersByBuyer(String buyerId) {
        return sorted(orderRepository.findByBuyerId(buyerId));
    }

    public List<Order> getOrdersByTransporter(String transporterId, String status) {
        List<Order> all = new ArrayList<>(orderRepository.findByTransporterId(transporterId));
        for (Order o : orderRepository.findByStatusAndOfferedTransporterIds("TRANSPORTER_ASSIGNED", transporterId)) {
            if (all.stream().noneMatch(x -> x.getOrderId().equals(o.getOrderId()))) all.add(o); // open broadcast requests
        }
        return sorted(status == null || status.isBlank() ? all
                : all.stream().filter(o -> status.equalsIgnoreCase(o.getStatus())).toList());
    }

    private static List<Order> sorted(List<Order> list) {
        List<Order> copy = new ArrayList<>(list);
        copy.sort((a, b) -> String.valueOf(b.getCreatedDate()).compareTo(String.valueOf(a.getCreatedDate())));
        return copy;
    }

    // ------------------------------------------------------------------ 2.1 order creation

    /** Scenario A: buyer sends a purchase request for a verified listing. */
    public Order createOrder(Order order) {
        Listing listing = order.getListingId() == null ? null : listings.findById(order.getListingId()).orElse(null);
        if (listing == null || !"ACTIVE".equals(listing.getStatus())) {
            throw new IllegalStateException("Listing is not available");
        }
        checkQuantity(order.getQuantity(), listing);
        order.setFarmerId(listing.getFarmerId());
        order.setFarmerName(listing.getFarmerName());
        order.setCropName(listing.getCropName());
        order.setOrigin("PURCHASE_REQUEST");
        return startNegotiation(order, "Purchase request from " + order.getBuyerName());
    }

    /** Scenario B: buyer accepts a farmer's supply offer on their wanted ad. */
    public Order acceptSupplyOffer(String offerId, String buyerId, String buyerName) {
        SupplyOffer offer = offers.findById(offerId).orElseThrow(() -> new IllegalStateException("Offer not found"));
        WantedAd ad = offer.getRequestId() == null ? null : wantedAds.findById(offer.getRequestId()).orElse(null);
        if (ad == null || !buyerId.equals(ad.getBuyerId())) throw new IllegalStateException("This offer is not for your request");
        if (!"PENDING".equals(offer.getStatus())) throw new IllegalStateException("Offer was already " + offer.getStatus());
        Listing listing = offer.getBatchId() == null ? null : listings.findById(offer.getBatchId()).orElse(null);
        if (listing == null || !"ACTIVE".equals(listing.getStatus())) throw new IllegalStateException("The offered batch is no longer available");
        Order o = new Order();
        o.setBuyerId(buyerId);
        o.setBuyerName(buyerName);
        o.setListingId(listing.getId());
        o.setFarmerId(listing.getFarmerId());
        o.setFarmerName(listing.getFarmerName());
        o.setCropName(listing.getCropName());
        o.setQuantity(offer.getQuantity());
        o.setPricePerKg(offer.getPricePerKg());
        o.setOrigin("SUPPLY_OFFER");
        o.setSupplyOfferId(offerId);
        o.setWantedAdId(ad.getRequestId());
        Order saved = startNegotiation(o, "Supply offer accepted by " + buyerName);
        offer.setStatus("ACCEPTED");
        offers.save(offer);
        // The wanted ad closes when an offer is accepted; other pending offers are declined
        ad.setStatus("CLOSED");
        ad.setClosedDate(now());
        ad.setClosedReason("OFFER_ACCEPTED");
        wantedAds.save(ad);
        for (SupplyOffer other : offers.findByRequestId(ad.getRequestId())) {
            if (!other.getId().equals(offerId) && "PENDING".equals(other.getStatus())) {
                other.setStatus("DECLINED");
                offers.save(other);
                notifications.notify(other.getFarmerId(), "ORDERS", "OFFER_DECLINED", "Offer not selected",
                        buyerName + " accepted another offer for " + ad.getCropName() + ".");
            }
        }
        return saved;
    }

    private void checkQuantity(double qty, Listing listing) {
        if (qty <= 0) throw new IllegalStateException("Quantity must be positive");
        double available = listing.quantityKgValue();
        if (available > 0 && qty > available * 1.05) { // estimated quantity, ±5%
            throw new IllegalStateException(String.format("Only about %,.0f kg is available", available));
        }
        if (qty > FeeCalculator.maxCapacity(rules)) FeeCalculator.selectVehicle(qty, rules); // throws with message
    }

    private Order startNegotiation(Order o, String note) {
        o.setOrderId(null);
        o.setCreatedDate(now());
        o.setTotalAmount(FeeCalculator.round2(o.getQuantity() * o.getPricePerKg()));
        o.setStatusHistory(new ArrayList<>());
        o.setFarmerConfirmed(false);
        o.setBuyerConfirmed(false);
        setStatus(o, "NEGOTIATING", note);
        Order saved = orderRepository.save(o);
        notifications.notify(saved.getFarmerId(), "ORDERS", "ORDER_REQUEST", "New buyer request",
                String.format("%s wants %,.0f kg of %s at LKR %,.0f/kg. Open the chat to agree the final terms.",
                        saved.getBuyerName(), saved.getQuantity(), saved.getCropName(), saved.getPricePerKg()));
        return saved;
    }

    // ------------------------------------------------------------------ 2.2 negotiation

    /**
     * A party confirms the final terms (quantity, price per kg, pickup date). Changing a term resets the
     * other party's confirmation (counter-offer). When both have confirmed the same terms the order is
     * Accepted: logistics are planned and the advance is quoted.
     */
    public GenericResponse confirmDeal(String orderId, ConfirmDealRequest req, String userId) {
        Order o = require(orderId);
        expect(o, "NEGOTIATING");
        boolean isFarmer = userId.equals(o.getFarmerId());
        boolean isBuyer = userId.equals(o.getBuyerId());
        if (!isFarmer && !isBuyer) throw new IllegalStateException("Only the farmer or buyer can confirm the deal");
        if (req.getQuantity() <= 0 || req.getPricePerKg() <= 0) throw new IllegalStateException("Quantity and price must be positive");
        Listing listing = o.getListingId() == null ? null : listings.findById(o.getListingId()).orElse(null);
        if (listing != null) checkQuantity(req.getQuantity(), listing);

        boolean changed = Math.abs(req.getQuantity() - o.getQuantity()) > 1e-6
                || Math.abs(req.getPricePerKg() - o.getPricePerKg()) > 1e-6
                || (req.getDeliveryDate() != null && !req.getDeliveryDate().equals(o.getPickupDate()));
        if (changed) {
            o.setQuantity(req.getQuantity());
            o.setPricePerKg(req.getPricePerKg());
            if (req.getDeliveryDate() != null) {
                o.setPickupDate(req.getDeliveryDate());
                o.setDeliveryDate(req.getDeliveryDate());
            }
            o.setTotalAmount(FeeCalculator.round2(o.getQuantity() * o.getPricePerKg()));
            o.setFarmerConfirmed(false);
            o.setBuyerConfirmed(false);
        }
        if (isFarmer) o.setFarmerConfirmed(true);
        if (isBuyer) o.setBuyerConfirmed(true);
        if (o.getPickupDate() == null) throw new IllegalStateException("Set a pickup date");

        if (o.isFarmerConfirmed() && o.isBuyerConfirmed()) {
            accept(o);
            return new GenericResponse(true, "Deal accepted. Waiting for the buyer's advance payment.");
        }
        orderRepository.save(o);
        String other = isFarmer ? o.getBuyerId() : o.getFarmerId();
        notifications.notify(other, "ORDERS", "DEAL_CONFIRMATION", "Please confirm the deal",
                String.format("%,.0f kg %s at LKR %,.0f/kg, pickup %s. Confirm to lock the order.",
                        o.getQuantity(), o.getCropName(), o.getPricePerKg(), o.getPickupDate()));
        return new GenericResponse(true, isFarmer ? "Sent to the buyer for confirmation." : "Sent to the farmer for confirmation.");
    }

    private void accept(Order o) {
        User farmer = users.findById(o.getFarmerId()).orElseThrow(() -> new IllegalStateException("Farmer not found"));
        User buyer = users.findById(o.getBuyerId()).orElseThrow(() -> new IllegalStateException("Buyer not found"));
        double[] from = DispatchService.position(farmer);
        double[] to = DispatchService.position(buyer);
        if (from == null) throw new IllegalStateException("The farmer has no farm location");
        if (to == null) throw new IllegalStateException("The buyer has no delivery location");

        MarketplaceRules.Vehicle v = FeeCalculator.selectVehicle(o.getQuantity(), rules);
        DistanceService.Result d = distance.distance(from[0], from[1], to[0], to[1]);
        LogisticsRecord lr = logistics.findByOrderId(o.getOrderId()).orElseGet(LogisticsRecord::new);
        lr.setOrderId(o.getOrderId());
        lr.setWeightKg(o.getQuantity());
        lr.setVehicleType(v.getType());
        lr.setVehicleLabel(v.getLabel());
        lr.setVehicleCapacityKg(v.getCapacityKg());
        lr.setRatePerKm(v.getRatePerKm());
        lr.setHandlingFeePerKg(rules.getHandlingFeePerKg());
        lr.setPickupLatitude(from[0]);
        lr.setPickupLongitude(from[1]);
        lr.setDropLatitude(to[0]);
        lr.setDropLongitude(to[1]);
        lr.setDistanceKm(d.km());
        lr.setDistanceSource(d.source());
        lr.setDistanceCost(FeeCalculator.distanceCost(d.km(), v));
        lr.setHandlingCost(FeeCalculator.handlingCost(o.getQuantity(), rules));
        lr.setTransportFee(FeeCalculator.round2(lr.getDistanceCost() + lr.getHandlingCost()));
        lr.setCreatedAt(now());
        logistics.save(lr);

        o.setPayment(FeeCalculator.quote(o.getQuantity(), o.getPricePerKg(), lr.getTransportFee(), rules));
        o.setVehicleType(v.getType());
        o.setVehicleLabel(v.getLabel());
        o.setDistanceKm(d.km());
        o.setPickupAddress(farmer.getFarmLocation() == null ? null : farmer.getFarmLocation().getAddress());
        o.setDropAddress(buyer.getFarmLocation() == null ? null : buyer.getFarmLocation().getAddress());
        o.setEscrowStatus("AWAITING_PAYMENT");
        setStatus(o, "AWAITING_PAYMENT", String.format("Deal accepted. %s, %.1f km, transport LKR %s",
                v.getLabel(), d.km(), LedgerService.format(lr.getTransportFee())));
        orderRepository.save(o);

        listings.findById(o.getListingId() == null ? "" : o.getListingId()).ifPresent(l -> {
            l.setStatus("ONGOING");
            l.setActiveOrderId(o.getOrderId());
            l.setBuyerName(o.getBuyerName());
            l.setUpdatedTime(now());
            listings.save(l);
        });
        notifications.notify(o.getBuyerId(), "PAYMENTS", "PAYMENT_DUE", "Advance payment due",
                "Pay LKR " + LedgerService.format(o.getPayment().getAdvanceDue()) + " to lock order " + o.getOrderId() + ".");
        notifications.notify(o.getFarmerId(), "ORDERS", "DEAL_ACCEPTED", "Deal accepted",
                o.getBuyerName() + " confirmed the deal. Waiting for the advance payment. Do not harvest yet.");
    }

    public Order cancel(String orderId, String userId) {
        Order o = require(orderId);
        expect(o, "NEGOTIATING", "AWAITING_PAYMENT");
        if (!userId.equals(o.getFarmerId()) && !userId.equals(o.getBuyerId())) throw new IllegalStateException("Not your order");
        setStatus(o, "CANCELLED", "Cancelled before payment");
        listings.findById(o.getListingId() == null ? "" : o.getListingId()).ifPresent(l -> {
            if ("ONGOING".equals(l.getStatus()) && orderId.equals(l.getActiveOrderId())) {
                l.setStatus("ACTIVE");
                l.setActiveOrderId(null);
                listings.save(l);
            }
        });
        String other = userId.equals(o.getFarmerId()) ? o.getBuyerId() : o.getFarmerId();
        notifications.notify(other, "ORDERS", "ORDER_CANCELLED", "Order cancelled", "Order " + orderId + " was cancelled.");
        return orderRepository.save(o);
    }

    // ------------------------------------------------------------------ 3.2 advance payment

    public Order recordAdvancePayment(String orderId, double amount, String buyerId) {
        return recordAdvancePayment(orderId, amount, buyerId, "CARD");
    }

    /** method: CARD (simulated gateway) or WALLET (buyer's mock wallet balance) - . */
    public Order recordAdvancePayment(String orderId, double amount, String buyerId, String method) {
        Order o = require(orderId);
        if (!buyerId.equals(o.getBuyerId())) throw new IllegalStateException("Only the buyer can pay");
        expect(o, "AWAITING_PAYMENT");
        Order.Payment p = pay(o);
        if (Math.abs(amount - p.getAdvanceDue()) > 0.01) {
            throw new IllegalStateException("Advance payment must be exactly LKR " + LedgerService.format(p.getAdvanceDue()));
        }
        String m = normaliseMethod(method);
        ledger.payIntoEscrow(buyerId, m, p.getAdvanceDue(), "ADVANCE_PAYMENT", "Advance payment – order " + orderId, orderId);
        o.setAdvancePaymentMethod(m);
        p.setAdvancePaid(true);
        o.setEscrowStatus("ADVANCE_HELD");
        setStatus(o, "DISPATCHING", "Advance payment held in escrow");
        orderRepository.save(o);
        notifications.notify(o.getFarmerId(), "PAYMENTS", "ADVANCE_RECEIVED", "Advance payment received",
                "LKR " + LedgerService.format(p.getAdvanceDue()) + " is secured in escrow. A vehicle is being assigned – "
                        + "start harvesting only after the 'Vehicle Assigned' notification.");
        return dispatch.dispatch(o);
    }

    // ------------------------------------------------------------------ farmer / buyer events

    /** FARMER: START_HARVESTING, HARVEST_READY · BUYER: APPROVE_LOADING, REJECT_LOADING, ACCEPT_DELIVERY, REJECT_DELIVERY, GOODS_RECEIVED */
    public Order applyPartyEvent(String orderId, String event, String userId, String reason) {
        return applyPartyEvent(orderId, event, userId, reason, "CARD");
    }

    public Order applyPartyEvent(String orderId, String event, String userId, String reason, String method) {
        Order o = require(orderId);
        boolean farmer = userId.equals(o.getFarmerId());
        boolean buyer = userId.equals(o.getBuyerId());
        switch (event == null ? "" : event) {
            case "START_HARVESTING" -> {
                if (!farmer) throw new IllegalStateException("Only the farmer can start harvesting");
                expect(o, "TRANSPORTER_ACCEPTED");
                setStatus(o, "HARVESTING", "Farmer started harvesting");
            }
            case "HARVEST_READY" -> {
                if (!farmer) throw new IllegalStateException("Only the farmer can mark the harvest ready");
                expect(o, "HARVESTING");
                setStatus(o, "HARVEST_READY", "Harvest packed and ready for pickup");
                LogisticsRecord lr = logistics.findByOrderId(orderId).orElse(null);
                notifications.notify(o.getTransporterId(), "ORDERS", "PICKUP_READY", "Harvest ready for pickup",
                        lr == null ? "Go to the farm for order " + orderId
                                : String.format("Pickup GPS: %.6f, %.6f (order %s)", lr.getPickupLatitude(), lr.getPickupLongitude(), orderId));
            }
            case "APPROVE_LOADING" -> {
                if (!buyer) throw new IllegalStateException("Only the buyer can approve the loading photos");
                expect(o, "AWAITING_BUYER_LOADING_APPROVAL");
                approveLoading(o, false);
            }
            case "REJECT_LOADING" -> {
                if (!buyer) throw new IllegalStateException("Only the buyer can reject the loading photos");
                expect(o, "AWAITING_BUYER_LOADING_APPROVAL");
                settlePreloadRejection(o, reason);
            }
            case "ACCEPT_DELIVERY" -> {
                if (!buyer) throw new IllegalStateException("Only the buyer can accept the delivery");
                expect(o, "ARRIVED_AT_DESTINATION");
                Order.Payment p = pay(o);
                String m = normaliseMethod(method);
                ledger.payIntoEscrow(userId, m, p.getBalanceDue(), "BALANCE_PAYMENT",
                        "Balance payment (80%) – order " + orderId, orderId);
                o.setBalancePaymentMethod(m);
                p.setBalancePaid(true);
                o.setEscrowStatus("FULLY_PAID");
                setStatus(o, "BALANCE_PAID", "Buyer accepted the goods and paid the balance");
            }
            case "REJECT_DELIVERY" -> {
                if (!buyer) throw new IllegalStateException("Only the buyer can reject the delivery");
                expect(o, "ARRIVED_AT_DESTINATION");
                settlePostDeliveryRejection(o, reason);
            }
            case "GOODS_RECEIVED" -> {
                if (!buyer) throw new IllegalStateException("Only the buyer can confirm receipt");
                expect(o, "BALANCE_PAID", "DELIVERED");
                o.setBuyerConfirmedReceipt(true);
                afterDeliveryConfirmation(o, "Buyer confirmed goods received");
            }
            default -> throw new IllegalStateException("Unknown event " + event);
        }
        return orderRepository.save(o);
    }

    private static String normaliseMethod(String method) {
        if (method == null || method.isBlank()) return "CARD";
        String m = method.trim().toUpperCase();
        if (!m.equals("CARD") && !m.equals("WALLET")) throw new IllegalStateException("Payment method must be CARD or WALLET");
        return m;
    }

    private void approveLoading(Order o, boolean automatic) {
        o.setLoadingAutoApproved(automatic);
        setStatus(o, "LOADING_APPROVED", automatic
                ? "Pre-loading photos approved automatically (no buyer response in " + rules.getLoadingApprovalMinutes() + " min)"
                : "Buyer approved the pre-loading photos");
        notifications.notify(o.getTransporterId(), "ORDERS", "LOADING_APPROVED", "Loading approved",
                "The crop photos were approved. Load the goods and start the journey.");
        if (automatic) {
            notifications.notify(o.getBuyerId(), "ORDERS", "LOADING_AUTO_APPROVED", "Loading approved automatically",
                    "You did not respond within " + rules.getLoadingApprovalMinutes() + " minutes, so the crop photos for order "
                            + o.getOrderId() + " were approved and loading has started.");
        }
    }

    /** Pre-loading photos are auto-approved when the buyer does not answer in time. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${harvesthub.loading-check-ms:60000}", initialDelay = 30000)
    public void autoApproveExpiredLoading() {
        long nowMs = System.currentTimeMillis();
        for (Order o : orderRepository.findByStatus("AWAITING_BUYER_LOADING_APPROVAL")) {
            if (o.getLoadingApprovalDeadline() != null && o.getLoadingApprovalDeadline() <= nowMs) {
                approveLoading(o, true);
                orderRepository.save(o);
            }
        }
    }

    /** Declined or expired job request: remember the drivers, then send it to the next nearest ones. */
    private Order reroute(Order o, java.util.Collection<String> skip, String note) {
        LogisticsRecord lr = logistics.findByOrderId(o.getOrderId()).orElseThrow();
        for (String id : skip) if (id != null && !lr.getDeclinedTransporterIds().contains(id)) lr.getDeclinedTransporterIds().add(id);
        o.setOfferedTransporterIds(new ArrayList<>());
        lr.setTransporterId(null);
        logistics.save(lr);
        o.setTransporterId(null);
        o.setTransporterName(null);
        o.setTransporterPhone(null);
        o.setVehiclePlate(null);
        o.setTransportRequestExpiresAt(null);
        setStatus(o, "DISPATCHING", note);
        return dispatch.dispatch(o);
    }

    /** An unanswered job request expires after harvesthub.rules.transport-request-minutes. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${harvesthub.request-check-ms:30000}", initialDelay = 30000)
    public void expireTransportRequests() {
        long nowMs = System.currentTimeMillis();
        for (Order o : orderRepository.findByStatus("TRANSPORTER_ASSIGNED")) {
            if (o.getTransportRequestExpiresAt() == null || o.getTransportRequestExpiresAt() > nowMs) continue;
            java.util.List<String> waiting = new ArrayList<>(o.getOfferedTransporterIds() == null ? java.util.List.of() : o.getOfferedTransporterIds());
            if (o.getTransporterId() != null && !waiting.contains(o.getTransporterId())) waiting.add(o.getTransporterId());
            try {
                reroute(o, waiting, "Job request expired – rerouting");
                for (String tid : waiting) notifications.notify(tid, "ORDERS", "REQUEST_EXPIRED", "Job request expired",
                        "The request for order " + o.getOrderId() + " was not accepted in time, so it went to other drivers.");
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(OrderService.class).warn("Expiry of {} failed: {}", o.getOrderId(), e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ transporter events

    /** synchronized: when a request is broadcast, two drivers may accept at the same moment - only the first wins. */
    public synchronized Order applyTransporterEvent(String orderId, TransporterEventRequest request) {
        Order o = require(orderId);
        String event = request.getEventType() == null ? "" : request.getEventType();
        String tid = request.getTransporterId();
        boolean offered = "TRANSPORTER_ASSIGNED".equals(o.getStatus()) && o.getOfferedTransporterIds() != null
                && o.getOfferedTransporterIds().contains(tid) && ("ACCEPT".equals(event) || "DECLINE".equals(event));
        if (tid == null || !(tid.equals(o.getTransporterId()) || offered)) {
            if ("ACCEPT".equals(event) && tid != null && o.getTransporterId() != null) {
                throw new IllegalStateException("Another driver accepted this job first");
            }
            throw new IllegalStateException("You are not assigned to this order");
        }
        record(o, request);
        switch (event) {
            case "ACCEPT" -> {
                expect(o, "TRANSPORTER_ASSIGNED");
                if (o.getTransportRequestExpiresAt() != null && System.currentTimeMillis() > o.getTransportRequestExpiresAt()) {
                    throw new IllegalStateException("This job request has expired and was sent to another driver");
                }
                o.setTransportRequestExpiresAt(null);
                java.util.List<String> others = new ArrayList<>(o.getOfferedTransporterIds() == null ? java.util.List.of() : o.getOfferedTransporterIds());
                others.remove(tid);
                if (o.getTransporterId() == null) { // broadcast request: this driver was first
                    User t = users.findById(tid).orElseThrow(() -> new IllegalStateException("Driver not found"));
                    o.setTransporterId(tid);
                    o.setTransporterName(t.getFullName());
                    o.setTransporterPhone(t.getMobileNumber());
                    o.setVehiclePlate(t.getVehiclePlate());
                    logistics.findByOrderId(o.getOrderId()).ifPresent(lr -> { lr.setTransporterId(tid); logistics.save(lr); });
                }
                o.setOfferedTransporterIds(new ArrayList<>());
                for (String other : others) notifications.notify(other, "ORDERS", "REQUEST_TAKEN", "Job taken",
                        "Another driver accepted order " + orderId + " first.");
                setStatus(o, "TRANSPORTER_ACCEPTED", "Transporter accepted the job");
                users.findById(tid).ifPresent(t -> { t.setAvailable(false); users.save(t); });
                notifications.notify(o.getFarmerId(), "ORDERS", "VEHICLE_ASSIGNED", "Vehicle Assigned - Start Harvesting",
                        String.format("%s (%s) will collect %,.0f kg of %s. You can start harvesting now.",
                                o.getTransporterName(), o.getVehiclePlate() == null ? o.getVehicleLabel() : o.getVehiclePlate(),
                                o.getQuantity(), o.getCropName()));
                notifications.notify(o.getBuyerId(), "ORDERS", "VEHICLE_ASSIGNED", "Vehicle assigned",
                        o.getTransporterName() + " accepted the delivery of order " + orderId + ".");
            }
            case "DECLINE" -> {
                expect(o, "TRANSPORTER_ASSIGNED");
                if (o.getTransporterId() == null && o.getOfferedTransporterIds() != null && o.getOfferedTransporterIds().size() > 1) {
                    // broadcast request: drop this driver; the others can still accept
                    o.getOfferedTransporterIds().remove(tid);
                    logistics.findByOrderId(o.getOrderId()).ifPresent(lr -> {
                        if (!lr.getDeclinedTransporterIds().contains(tid)) lr.getDeclinedTransporterIds().add(tid);
                        logistics.save(lr);
                    });
                    return orderRepository.save(o);
                }
                return reroute(o, java.util.List.of(tid), "Transporter declined – rerouting");
            }
            case "LOCATION" -> {
                // live tracking point only; no status change
            }
            case "ARRIVED_PICKUP" -> {
                expect(o, "HARVEST_READY");
                setStatus(o, "ARRIVED_AT_PICKUP", "Vehicle arrived at the farm");
                notifications.notify(o.getFarmerId(), "ORDERS", "VEHICLE_ARRIVED", "Vehicle arrived",
                        o.getTransporterName() + " has arrived at your farm.");
            }
            case "LOADING_EVIDENCE_SUBMITTED" -> {
                expect(o, "ARRIVED_AT_PICKUP");
                java.util.List<String> urls = request.getPhotoUrls() == null ? java.util.List.of()
                        : request.getPhotoUrls().stream().filter(u -> u != null && !u.isBlank()).toList();
                int count = Math.max(urls.size(), request.getPhotoCount() == null ? 0 : request.getPhotoCount());
                if (count < rules.getMinLoadingPhotos()) {
                    throw new IllegalStateException("Take at least " + rules.getMinLoadingPhotos() + " pre-loading photos");
                }
                if (urls.size() > rules.getMaxLoadingPhotos()) {
                    throw new IllegalStateException("Upload at most " + rules.getMaxLoadingPhotos() + " photos");
                }
                o.setLoadingEvidenceUrls(new ArrayList<>(urls));
                o.setLoadingEvidenceAt(now());
                o.setLoadingApprovalDeadline(System.currentTimeMillis() + rules.getLoadingApprovalMinutes() * 60_000L);
                setStatus(o, "AWAITING_BUYER_LOADING_APPROVAL", "Pre-loading photos submitted");
                notifications.notify(o.getBuyerId(), "ORDERS", "LOADING_EVIDENCE", "Approve the crop photos",
                        "The transport helper uploaded pre-loading photos for order " + orderId + ". Approve or reject them within "
                                + rules.getLoadingApprovalMinutes() + " minutes, otherwise they are approved automatically.");
            }
            case "START_JOURNEY" -> {
                expect(o, "LOADING_APPROVED");
                Order.Payment p = pay(o);
                ledger.transfer(Wallet.ESCROW, o.getFarmerId(), p.getAdvanceCropPortion(), "CROP_ADVANCE",
                        "Crop advance released – order " + orderId, "Crop advance (20%) – order " + orderId, orderId);
                p.setFarmerAdvanceReleased(true);
                setStatus(o, "IN_TRANSIT", "Journey started – crop advance released to the farmer");
                notifications.notify(o.getFarmerId(), "PAYMENTS", "ADVANCE_RELEASED", "Advance payment released",
                        "LKR " + LedgerService.format(p.getAdvanceCropPortion()) + " was added to your wallet. Your crops are on the way.");
                notifications.notify(o.getBuyerId(), "ORDERS", "IN_TRANSIT", "Order in transit",
                        "Track order " + orderId + " live on the map.");
            }
            case "ARRIVED_DESTINATION" -> {
                expect(o, "IN_TRANSIT");
                o.setActualDeliveryDate(now());
                setStatus(o, "ARRIVED_AT_DESTINATION", "Vehicle arrived at the buyer");
                notifications.notify(o.getBuyerId(), "ORDERS", "ARRIVED", "Your order has arrived",
                        "Inspect the goods, then accept and pay the balance or reject the delivery.");
            }
            case "DELIVERY_COMPLETED" -> {
                expect(o, "BALANCE_PAID", "DELIVERED");
                o.setTransporterConfirmedDelivery(true);
                afterDeliveryConfirmation(o, "Driver confirmed delivery completed");
            }
            case "DELIVERY_REJECTED" -> {
                expect(o, "ARRIVED_AT_DESTINATION");
                settlePostDeliveryRejection(o, request.getRejectionReason());
            }
            case "GOODS_RETURNED" -> { // Rejected goods back at the farm; no money moves
                expect(o, "RETURN_JOURNEY");
                o.setGoodsReturnedAt(now());
                setStatus(o, "RETURNED", "Rejected goods returned to the farm");
                users.findById(tid).ifPresent(t -> { t.setAvailable(true); users.save(t); });
                notifications.notify(o.getFarmerId(), "ORDERS", "GOODS_RETURNED", "Goods returned",
                        o.getTransporterName() + " returned the rejected " + o.getCropName() + " (order " + orderId + ") to your farm.");
            }
            default -> throw new IllegalStateException("Unknown event " + event);
        }
        return orderRepository.save(o);
    }

    private void record(Order o, TransporterEventRequest r) {
        String ts = now();
        if (o.getTransportEvents() == null) o.setTransportEvents(new ArrayList<>());
        o.getTransportEvents().add(new Order.TransportEvent(r.getEventType(), ts, r.getTransporterId(), r.getNote(),
                r.getLatitude(), r.getLongitude(), r.getPhotoCount(), r.getBuyerVerified(), r.getRejectionReason()));
        if (r.getLatitude() != null && r.getLongitude() != null) {
            logistics.findByOrderId(o.getOrderId()).ifPresent(lr -> {
                lr.getTrackingPoints().add(new LogisticsRecord.TrackPoint(r.getLatitude(), r.getLongitude(), ts));
                logistics.save(lr);
            });
            users.findById(r.getTransporterId()).ifPresent(t -> {
                t.setLastLatitude(r.getLatitude());
                t.setLastLongitude(r.getLongitude());
                users.save(t);
            });
        }
    }

    // ------------------------------------------------------------------ 5.2 / 5.3 settlement

    private void afterDeliveryConfirmation(Order o, String note) {
        if (o.isTransporterConfirmedDelivery() && o.isBuyerConfirmedReceipt()) {
            settleCompletion(o);
        } else {
            setStatus(o, "DELIVERED", note + " – waiting for the other confirmation");
        }
    }

    /**platform keeps 3% + 5% commission; farmer gets the 80% balance; transporter 95% of the fee. */
    void settleCompletion(Order o) {
        Order.Payment p = pay(o);
        String id = o.getOrderId();
        ledger.transfer(Wallet.ESCROW, o.getFarmerId(), p.getBalanceDue(), "CROP_BALANCE",
                "Crop balance released – order " + id, "Final crop payment (80%) – order " + id, id);
        double payout = FeeCalculator.transporterPayout(p, rules);
        ledger.transfer(Wallet.ESCROW, o.getTransporterId(), payout, "TRANSPORT_PAYOUT",
                "Transport payout – order " + id, "Delivery fee (95%) – order " + id, id);
        double platform = FeeCalculator.round2(p.getBuyerFee() + FeeCalculator.platformCommission(p, rules));
        ledger.transfer(Wallet.ESCROW, Wallet.PLATFORM, platform, "PLATFORM_FEE",
                "Platform fees – order " + id, "3% buyer fee + 5% transport commission – order " + id, id);
        p.setSettled(true);
        p.setSettlementNote("Completed: farmer " + LedgerService.format(p.getAdvanceCropPortion() + p.getBalanceDue())
                + ", transporter " + LedgerService.format(payout) + ", platform " + LedgerService.format(platform));
        o.setEscrowStatus("RELEASED");
        setStatus(o, "COMPLETED", "2-way confirmation received – funds released");
        finish(o, "COMPLETED");
        notifications.notify(o.getFarmerId(), "PAYMENTS", "DELIVERY_SUCCESS", "Delivery Successful - Funds Transferred",
                "LKR " + LedgerService.format(p.getBalanceDue()) + " has been credited to your wallet for order " + id + ".");
        notifications.notify(o.getTransporterId(), "PAYMENTS", "DELIVERY_SUCCESS", "Delivery fee paid",
                "LKR " + LedgerService.format(payout) + " has been credited to your wallet.");
        notifications.notify(o.getBuyerId(), "ORDERS", "COMPLETED", "Order completed", "Please rate the farmer and the transporter.");
    }

    /**rejected BEFORE loading: farmer at fault. */
    void settlePreloadRejection(Order o, String reason) {
        Order.Payment p = pay(o);
        String id = o.getOrderId();
        ledger.transfer(Wallet.ESCROW, o.getBuyerId(), p.getAdvanceDue(), "REFUND",
                "Refund to buyer – order " + id, "Full refund incl. 3% fee – order " + id, id);
        double fee = p.getTransportFee(); // Empty Trip Fee = full transport fee
        ledger.transfer(o.getFarmerId(), o.getTransporterId(), fee, "EMPTY_TRIP_FEE",
                "Empty Trip Fee – order " + id, "Empty Trip Fee received – order " + id, id);
        ledger.addPenalty(o.getFarmerId(), fee);
        p.setEmptyTripFee(fee);
        p.setSettled(true);
        p.setSettlementNote("Rejected before loading" + (reason == null ? "" : ": " + reason));
        o.setEscrowStatus("REFUNDED");
        setStatus(o, "PRELOAD_REJECTED", "Buyer rejected the pre-loading photos" + (reason == null ? "" : ": " + reason));
        finish(o, "CLOSED");
        boolean suspended = ledger.wallet(o.getFarmerId()).isSuspended();
        notifications.notify(o.getFarmerId(), "PAYMENTS", "PENALTY", "Penalty Alert: Empty Trip Fee",
                "The buyer rejected the crops before loading. An Empty Trip Fee of LKR " + LedgerService.format(fee)
                        + " was charged." + (suspended ? " Your balance is negative – you cannot create listings until it is paid back." : ""));
        notifications.notify(o.getBuyerId(), "PAYMENTS", "REFUND", "Full refund issued",
                "LKR " + LedgerService.format(p.getAdvanceDue()) + " was refunded to your wallet.");
        notifications.notify(o.getTransporterId(), "PAYMENTS", "EMPTY_TRIP", "Empty Trip Fee received",
                "LKR " + LedgerService.format(fee) + " for the wasted trip on order " + id + ".");
    }

    /**rejected AFTER delivery: transporter at fault. */
    void settlePostDeliveryRejection(Order o, String reason) {
        Order.Payment p = pay(o);
        String id = o.getOrderId();
        ledger.transfer(Wallet.ESCROW, o.getBuyerId(), FeeCalculator.round2(p.getTransportFee() + p.getBuyerFee()), "REFUND",
                "Refund to buyer – order " + id, "Refund of delivery fee + 3% fee – order " + id, id);
        ledger.transfer(o.getTransporterId(), o.getBuyerId(), p.getAdvanceCropPortion(), "DAMAGE_CHARGE",
                "Damaged goods charge – order " + id, "Crop money refunded by transporter – order " + id, id);
        ledger.addPenalty(o.getTransporterId(), p.getAdvanceCropPortion());
        p.setSettled(true);
        p.setSettlementNote("Rejected after delivery" + (reason == null ? "" : ": " + reason));
        o.setEscrowStatus("REFUNDED");
        setStatus(o, "RETURN_JOURNEY", "Delivery rejected – return journey" + (reason == null ? "" : ": " + reason));
        finish(o, "CLOSED");
        notifications.notify(o.getFarmerId(), "ORDERS", "DELIVERY_REJECTED", "Delivery rejected by the buyer",
                "The goods were damaged in transit. You keep the advance of LKR " + LedgerService.format(p.getAdvanceCropPortion()) + ".");
        notifications.notify(o.getTransporterId(), "PAYMENTS", "PENALTY", "Damage charge",
                "LKR " + LedgerService.format(p.getAdvanceCropPortion()) + " was charged for the damaged goods. No delivery fee is paid.");
        notifications.notify(o.getBuyerId(), "PAYMENTS", "REFUND", "Full refund issued", "All money for order " + id + " was refunded.");
    }

    private void finish(Order o, String listingStatus) {
        listings.findById(o.getListingId() == null ? "" : o.getListingId()).ifPresent(l -> {
            l.setStatus(listingStatus);
            l.setActiveOrderId(null);
            l.setFinalPricePerKg(String.valueOf(o.getPricePerKg()));
            l.setQuantityKg(String.valueOf(o.getQuantity()));
            l.setTotalValue(LedgerService.format(o.getPayment() == null ? o.getTotalAmount() : o.getPayment().getCropValue()));
            l.setBuyerName(o.getBuyerName());
            l.setUpdatedTime(now());
            if ("PRELOAD_REJECTED".equals(o.getStatus())) l.setReviewNotes("Rejected by the buyer at the pre-loading check");
            listings.save(l);
        });
        if (o.getTransporterId() != null && !"RETURN_JOURNEY".equals(o.getStatus())) { // return trip still to drive
            users.findById(o.getTransporterId()).ifPresent(t -> { t.setAvailable(true); users.save(t); });
        }
        orderRepository.save(o);
        for (String uid : new String[]{o.getFarmerId(), o.getBuyerId(), o.getTransporterId()}) {
            if (uid != null) trust.recompute(uid);
        }
    }

    // ------------------------------------------------------------------ 5.4 rating

    /** Rates the other party with the given role (BUYER, FARMER or TRANSPORTER) after the order ended. */
    public Rating rate(String orderId, String fromUserId, String toRole, int stars, String comment) {
        Order o = require(orderId);
        if (!FINAL.contains(o.getStatus()) || "CANCELLED".equals(o.getStatus())) {
            throw new IllegalStateException("You can rate after the order is finished");
        }
        if (stars < 1 || stars > 5) throw new IllegalStateException("Rating must be 1 to 5 stars");
        String toUserId = switch (toRole == null ? "" : toRole.toUpperCase()) {
            case "BUYER" -> o.getBuyerId();
            case "FARMER" -> o.getFarmerId();
            case "TRANSPORTER" -> o.getTransporterId();
            default -> throw new IllegalStateException("toRole must be BUYER, FARMER or TRANSPORTER");
        };
        if (toUserId == null || toUserId.equals(fromUserId)) throw new IllegalStateException("You cannot rate this user");
        if (ratings.existsByOrderIdAndFromUserIdAndToUserId(orderId, fromUserId, toUserId)) {
            throw new IllegalStateException("You already rated this user for this order");
        }
        Rating r = new Rating(null, orderId, fromUserId, toUserId, toRole.toUpperCase(), stars, comment, now());
        Rating saved = ratings.save(r);
        if (o.getRatedBy() == null) o.setRatedBy(new ArrayList<>());
        if (!o.getRatedBy().contains(fromUserId)) o.getRatedBy().add(fromUserId);
        if (fromUserId.equals(o.getBuyerId()) && "FARMER".equalsIgnoreCase(toRole)) {
            o.setRating(stars); // legacy field shown in the farmer's history
            o.setRatingComment(comment);
        }
        orderRepository.save(o);
        trust.recompute(toUserId);
        return saved;
    }
}
