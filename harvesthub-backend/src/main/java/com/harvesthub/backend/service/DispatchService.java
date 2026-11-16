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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Smart dispatch (workflow 3.3): after the advance payment, offer the job to the nearest available
 * transporter whose vehicle can carry the load. Declines go to the next driver; with no driver the order
 * waits as PAUSED and is retried every 2 minutes (proposal §6.9 "Paused").
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
        Optional<User> best = candidates(order.getQuantity(), lr).stream()
                .min(Comparator.comparingDouble(t -> distanceKm(t, lr)));
        if (best.isEmpty()) {
            if (!"PAUSED".equals(order.getStatus())) {
                OrderService.setStatus(order, "PAUSED", "No vehicle available – waiting for a transporter");
                notifications.notify(order.getFarmerId(), "ORDERS", "DISPATCH",
                        "Looking for a vehicle", "No transporter is free right now for order " + order.getOrderId()
                                + ". We will assign one as soon as a vehicle is online. Do not start harvesting yet.");
            }
            return orders.save(order);
        }
        User t = best.get();
        order.setTransporterId(t.getId());
        order.setTransporterName(t.getFullName());
        order.setTransporterPhone(t.getMobileNumber());
        order.setVehiclePlate(t.getVehiclePlate());
        lr.setTransporterId(t.getId());
        logistics.save(lr);
        order.setTransportRequestExpiresAt(System.currentTimeMillis() + rules.getTransportRequestMinutes() * 60_000L);
        OrderService.setStatus(order, "TRANSPORTER_ASSIGNED", "Transport request sent to " + t.getFullName());
        notifications.notify(t.getId(), "ORDERS", "TRANSPORT_REQUEST", "New delivery job",
                String.format("%s, %,.0f kg, %.1f km. Pickup %s. Accept or decline within " + rules.getTransportRequestMinutes() + " minutes in Jobs.",
                        order.getCropName(), order.getQuantity(), lr.getDistanceKm(),
                        order.getPickupAddress() == null ? "at the farm" : order.getPickupAddress()));
        return orders.save(order);
    }

    List<User> candidates(double weightKg, LogisticsRecord lr) {
        return users.findByRole("TRANSPORTER").stream()
                .filter(t -> !Boolean.FALSE.equals(t.getAvailable()))
                .filter(t -> !Boolean.FALSE.equals(t.getOnline()))
                .filter(t -> !t.isAccountSuspended())
                .filter(t -> !lr.getDeclinedTransporterIds().contains(t.getId()))
                .filter(t -> capacityOf(t) >= weightKg)
                .filter(t -> wallets.findByUserId(t.getId()).map(w -> !w.isSuspended()).orElse(true))
                .filter(t -> position(t) != null)
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
