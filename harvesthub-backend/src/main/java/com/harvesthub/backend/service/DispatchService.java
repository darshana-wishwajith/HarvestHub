package com.harvesthub.backend.service;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.model.LogisticsRecord;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.LogisticsRepository;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Smart dispatch: after the advance payment, the job request is broadcast to the nearest available
 * transporters whose vehicle can carry the load (up to {@code dispatch-broadcast-size}); the first driver to accept
 * gets the order. If they all decline or the request
 * expires, it goes to the next nearest drivers; with no driver the order waits as PAUSED and is retried every
 * 2 minutes.
 */
@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final UserRepository users;
    private final OrderRepository orders;
    private final LogisticsRepository logistics;
    private final WalletRepository wallets;
    private final NotificationService notifications;
    private final MarketplaceRules rules;

    public DispatchService(UserRepository users, OrderRepository orders, LogisticsRepository logistics,
                           WalletRepository wallets, NotificationService notifications, MarketplaceRules rules) {
        this.users = users;
        this.orders = orders;
        this.logistics = logistics;
        this.wallets = wallets;
        this.notifications = notifications;
        this.rules = rules;
    }

    /** Assigns the best transporter or parks the order as PAUSED. Saves and returns the order. */
    public Order dispatch(Order order) {
        LogisticsRecord lr = logistics.findByOrderId(order.getOrderId()).orElse(null);
        if (lr == null) throw new IllegalStateException("Order has no logistics plan");
        List<User> nearest = candidates(order.getQuantity(), lr).stream()
                .sorted(Comparator.comparingDouble(t -> distanceKm(t, lr)))
                .limit(Math.max(1, rules.getDispatchBroadcastSize()))
                .toList();
        if (nearest.isEmpty()) {
            if ("PAUSED".equals(order.getStatus()) && !lr.getDeclinedTransporterIds().isEmpty()) {
                // everyone who could carry it declined or let it expire: give them another chance on the next retry
                lr.getDeclinedTransporterIds().clear();
                logistics.save(lr);
            }
            if (!"PAUSED".equals(order.getStatus())) {
                OrderService.setStatus(order, "PAUSED", "No vehicle available – waiting for a transporter");
                notifications.notify(order.getFarmerId(), "ORDERS", "DISPATCH",
                        "Looking for a vehicle", "No transporter is free right now for order " + order.getOrderId()
                                + ". We will assign one as soon as a vehicle is online. Do not start harvesting yet.");
            }
            return orders.save(order);
        }
        // nobody owns the job until one of the drivers accepts it
        order.setTransporterId(null);
        order.setTransporterName(null);
        order.setTransporterPhone(null);
        order.setVehiclePlate(null);
        lr.setTransporterId(null);
        logistics.save(lr);
        List<String> ids = new ArrayList<>();
        nearest.forEach(t -> ids.add(t.getId()));
        order.setOfferedTransporterIds(ids);
        order.setTransportRequestExpiresAt(System.currentTimeMillis() + rules.getTransportRequestMinutes() * 60_000L);
        OrderService.setStatus(order, "TRANSPORTER_ASSIGNED", "Job request sent to " + ids.size()
                + (ids.size() == 1 ? " nearby driver" : " nearby drivers - first to accept gets it"));
        String msg = String.format("%s, %,.0f kg, %.1f km. Pickup %s. %s within %d minutes in Jobs.",
                order.getCropName(), order.getQuantity(), lr.getDistanceKm(),
                order.getPickupAddress() == null ? "at the farm" : order.getPickupAddress(),
                ids.size() == 1 ? "Accept or decline" : "Sent to " + ids.size() + " nearby drivers - the first to accept gets it. Answer",
                rules.getTransportRequestMinutes());
        for (String id : ids) notifications.notify(id, "ORDERS", "TRANSPORT_REQUEST", "New delivery job", msg);
        return orders.save(order);
    }

    /** Order stages in which the driver is still physically on a job. */
    static final java.util.Set<String> ON_A_JOB = java.util.Set.of("TRANSPORTER_ACCEPTED", "HARVESTING", "HARVEST_READY",
            "ARRIVED_AT_PICKUP", "AWAITING_BUYER_LOADING_APPROVAL", "LOADING_APPROVED", "IN_TRANSIT", "ARRIVED_AT_DESTINATION",
            "RETURN_JOURNEY");

    /**
     * Busy = has a job in progress, worked out from the driver's orders. The stored "available" flag was
     * only reset on completion / goods returned, so a driver whose last order ended any other way (buyer never pressed
     * Goods Received, pre-loading rejection, cancellation) stayed "busy" for ever and never got another request.
     * After the driver confirms the delivery they are free, even while the buyer's confirmation is pending.
     */
    boolean busy(User t) {
        for (Order o : orders.findByTransporterId(t.getId())) {
            String s = o.getStatus() == null ? "" : o.getStatus();
            if (ON_A_JOB.contains(s)) return true;
            if (("BALANCE_PAID".equals(s) || "DELIVERED".equals(s)) && !o.isTransporterConfirmedDelivery()) return true;
        }
        return false;
    }

    List<User> candidates(double weightKg, LogisticsRecord lr) {
        return users.findByRole("TRANSPORTER").stream()
                // Only drivers who switched "Available for Hauls" on
                .filter(t -> Boolean.TRUE.equals(t.getOnline()))
                .filter(t -> !t.isAccountSuspended())
                .filter(t -> !lr.getDeclinedTransporterIds().contains(t.getId()))
                .filter(t -> capacityOf(t) >= weightKg)
                .filter(t -> wallets.findByUserId(t.getId()).map(w -> !w.isSuspended()).orElse(true))
                .filter(t -> position(t) != null)
                .filter(t -> !busy(t))
                .toList();
    }

    double capacityOf(User t) {
        if (t.getVehicleCapacityKg() != null) return t.getVehicleCapacityKg();
        return rules.getVehicles().stream().filter(v -> v.getType().equals(t.getVehicleType()))
                .mapToDouble(MarketplaceRules.Vehicle::getCapacityKg).findFirst().orElse(0);
    }

    static double[] position(User u) {
        if (u.getLastLatitude() != null && u.getLastLongitude() != null) return new double[]{u.getLastLatitude(), u.getLastLongitude()};
        if (u.getFarmLocation() != null && u.getFarmLocation().getLatitude() != null)
            return new double[]{u.getFarmLocation().getLatitude(), u.getFarmLocation().getLongitude()};
        return null;
    }

    private double distanceKm(User t, LogisticsRecord lr) {
        double[] p = Objects.requireNonNull(position(t));
        if (lr.getPickupLatitude() == null) return 0;
        return FeeCalculator.haversineKm(p[0], p[1], lr.getPickupLatitude(), lr.getPickupLongitude());
    }

    /** Retry orders waiting for a vehicle. */
    @Scheduled(fixedDelayString = "${harvesthub.dispatch-retry-ms:120000}", initialDelay = 60000)
    public void retryPaused() {
        for (Order o : orders.findByStatus("PAUSED")) {
            try {
                dispatch(o);
            } catch (Exception e) {
                log.warn("Re-dispatch of {} failed: {}", o.getOrderId(), e.getMessage());
            }
        }
    }
}
