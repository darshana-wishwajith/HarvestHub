package com.harvesthub.backend.service;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.model.Order;

/**
 * Pure money rules (no I/O), so they are easy to unit test.
 * <ul>
 *   <li>Transport fee = Distance × Rate + Weight × HandlingFee (workflow 3.1)</li>
 *   <li>Buyer fee = 3% of (crop value + transport fee)</li>
 *   <li>Advance = 20% of crop value + transport fee + buyer fee (workflow 3.2; user decision 14 Nov)</li>
 *   <li>Balance = remaining 80% of crop value (workflow 5.1)</li>
 * </ul>
 */
public final class FeeCalculator {

    private FeeCalculator() {}

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static MarketplaceRules.Vehicle selectVehicle(double weightKg, MarketplaceRules rules) {
        if (weightKg <= 0) throw new IllegalArgumentException("Weight must be positive");
        return rules.getVehicles().stream()
                .filter(v -> v.getCapacityKg() >= weightKg)
                .min((a, b) -> Double.compare(a.getCapacityKg(), b.getCapacityKg()))
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "%,.0f kg is more than the largest vehicle can carry (%,.0f kg). Split the order.",
                        weightKg, maxCapacity(rules))));
    }

    public static double maxCapacity(MarketplaceRules rules) {
        return rules.getVehicles().stream().mapToDouble(MarketplaceRules.Vehicle::getCapacityKg).max().orElse(0);
    }

    public static double distanceCost(double distanceKm, MarketplaceRules.Vehicle vehicle) {
        return round2(distanceKm * vehicle.getRatePerKm());
    }

    public static double handlingCost(double weightKg, MarketplaceRules rules) {
        return round2(weightKg * rules.getHandlingFeePerKg());
    }

    public static Order.Payment quote(double quantityKg, double pricePerKg, double transportFee, MarketplaceRules rules) {
        double cropValue = round2(quantityKg * pricePerKg);
        double buyerFee = round2((cropValue + transportFee) * rules.getBuyerFeeRate());
        double advanceCrop = round2(cropValue * rules.getAdvanceCropRate());
        Order.Payment p = new Order.Payment();
        p.setCropValue(cropValue);
        p.setTransportFee(round2(transportFee));
        p.setBuyerFee(buyerFee);
        p.setAdvanceCropPortion(advanceCrop);
        p.setAdvanceDue(round2(advanceCrop + transportFee + buyerFee));
        p.setBalanceDue(round2(cropValue - advanceCrop));
        p.setTransporterPayout(round2(transportFee * (1 - rules.getTransporterCommissionRate())));
        return p;
    }

    /** Transporter's share at completion: transport fee minus the 5% platform commission. */
    public static double transporterPayout(Order.Payment p, MarketplaceRules rules) {
        return round2(p.getTransportFee() * (1 - rules.getTransporterCommissionRate()));
    }

    public static double platformCommission(Order.Payment p, MarketplaceRules rules) {
        return round2(p.getTransportFee() - transporterPayout(p, rules));
    }

    /** Great-circle distance in km. */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
