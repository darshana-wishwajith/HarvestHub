package com.harvesthub.backend.service;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.dto.TrackingResponse;
import com.harvesthub.backend.model.LogisticsRecord;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.repository.LogisticsRepository;
import com.harvesthub.backend.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Live delivery tracking. Positions come from the transporter app's
 * LOCATION events, stored as tracking points on the logistics record.
 */
@Service
public class TrackingService {

    private final OrderRepository orders;
    private final LogisticsRepository logistics;
    private final MarketplaceRules rules;

    public TrackingService(OrderRepository orders, LogisticsRepository logistics, MarketplaceRules rules) {
        this.orders = orders;
        this.logistics = logistics;
        this.rules = rules;
    }

    public TrackingResponse tracking(String orderId) {
        Order o = orders.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found"));
        LogisticsRecord lr = logistics.findByOrderId(orderId).orElse(null);
        TrackingResponse r = new TrackingResponse();
        r.setOrderId(orderId);
        r.setStatus(o.getStatus());
        r.setCropName(o.getCropName());
        r.setQuantityKg(o.getQuantity());
        r.setPickupAddress(o.getPickupAddress());
        r.setDropAddress(o.getDropAddress());
        r.setTransporterName(o.getTransporterName());
        r.setTransporterPhone(o.getTransporterPhone());
        r.setVehicleLabel(o.getVehicleLabel());
        r.setVehiclePlate(o.getVehiclePlate());
        r.setTotalKm(o.getDistanceKm() == null ? 0 : o.getDistanceKm());
        if (lr == null) {
            r.setProgressPercent(progressFromStatus(o.getStatus()));
            return r;
        }
        r.setPickupLatitude(lr.getPickupLatitude());
        r.setPickupLongitude(lr.getPickupLongitude());
        r.setDropLatitude(lr.getDropLatitude());
        r.setDropLongitude(lr.getDropLongitude());
        r.setTotalKm(lr.getDistanceKm());

        List<LogisticsRecord.TrackPoint> pts = lr.getTrackingPoints();
        r.setPointCount(pts == null ? 0 : pts.size());
        boolean moving = "IN_TRANSIT".equals(o.getStatus());
        if (pts != null && !pts.isEmpty()) {
            LogisticsRecord.TrackPoint last = pts.get(pts.size() - 1);
            r.setCurrentLatitude(last.getLatitude());
            r.setCurrentLongitude(last.getLongitude());
            r.setLastUpdate(last.getTimestamp());
            if (pts.size() >= 2) r.setSpeedKmh(speed(pts.get(pts.size() - 2), last));
            if (lr.getDropLatitude() != null && lr.getDropLongitude() != null) {
                double remaining = FeeCalculator.haversineKm(last.getLatitude(), last.getLongitude(),
                        lr.getDropLatitude(), lr.getDropLongitude()) * rules.getRoadDistanceFactor();
                if (lr.getDistanceKm() > 0) remaining = Math.min(remaining, lr.getDistanceKm());
                r.setRemainingKm(FeeCalculator.round2(remaining));
            }
        }
        if (moving) {
            double remaining = r.getRemainingKm() != null ? r.getRemainingKm() : r.getTotalKm();
            r.setRemainingKm(FeeCalculator.round2(remaining));
            double v = r.getSpeedKmh() != null && r.getSpeedKmh() >= 5 ? r.getSpeedKmh() : rules.getDefaultTruckSpeedKmh();
            r.setEtaMinutes((int) Math.ceil(remaining / v * 60));
            int pct = r.getTotalKm() > 0 ? (int) Math.round(100 * (1 - remaining / r.getTotalKm())) : 0;
            r.setProgressPercent(Math.max(5, Math.min(99, pct)));
        } else {
            r.setProgressPercent(progressFromStatus(o.getStatus()));
            if (r.getProgressPercent() == 100) {
                r.setRemainingKm(0.0);
                r.setEtaMinutes(0);
            }
        }
        return r;
    }

    private static Double speed(LogisticsRecord.TrackPoint a, LogisticsRecord.TrackPoint b) {
        try {
            long seconds = Duration.between(LocalDateTime.parse(a.getTimestamp()), LocalDateTime.parse(b.getTimestamp())).getSeconds();
            if (seconds <= 0) return null;
            double km = FeeCalculator.haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
            return FeeCalculator.round2(km / (seconds / 3600.0));
        } catch (Exception e) {
            return null;
        }
    }

    static int progressFromStatus(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "ARRIVED_AT_DESTINATION", "BALANCE_PAID", "DELIVERED", "COMPLETED" -> 100;
            case "LOADING_APPROVED" -> 2;
            default -> 0;
        };
    }
}
