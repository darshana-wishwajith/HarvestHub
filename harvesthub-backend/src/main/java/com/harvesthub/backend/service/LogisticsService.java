package com.harvesthub.backend.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LogisticsService {

    public Map<String, Object> calculateLogistics(String origin, String destination, double weightKg, String vehicleTier) {
        double distanceKm = estimateDistance(origin, destination);
        double ratePerKm;
        double baseRate;
        String tier = (vehicleTier != null && !vehicleTier.isBlank()) ? vehicleTier.toUpperCase() : "SMALL_LORRY";

        switch (tier) {
            case "THREE_WHEELER":
                ratePerKm = 65.0;
                baseRate = 1200.0;
                break;
            case "LARGE_TRUCK":
                ratePerKm = 140.0;
                baseRate = 6000.0;
                break;
            case "SMALL_LORRY":
            default:
                ratePerKm = 95.0;
                baseRate = 3500.0;
                tier = "SMALL_LORRY";
                break;
        }

        double transportCost = Math.round(baseRate + (distanceKm * ratePerKm));
        double transporterCommission = Math.round(transportCost * 0.05);
        double transporterNetPayout = transportCost - transporterCommission;

        Map<String, Object> result = new HashMap<>();
        result.put("origin", origin);
        result.put("destination", destination);
        result.put("distanceKm", distanceKm);
        result.put("vehicleTier", tier);
        result.put("transportCost", transportCost);
        result.put("transporterCommission", transporterCommission);
        result.put("transporterNetPayout", transporterNetPayout);

        return result;
    }

    private double estimateDistance(String origin, String destination) {
        if (origin == null || destination == null) return 120.0;
        String orig = origin.toLowerCase();
        String dest = destination.toLowerCase();

        if (orig.contains("nuwara eliya") && dest.contains("colombo")) return 165.0;
        if (orig.contains("dambulla") && dest.contains("colombo")) return 155.0;
        if (orig.contains("kandy") && dest.contains("colombo")) return 115.0;
        if (orig.contains("jaffna") && dest.contains("colombo")) return 395.0;
        if (orig.contains("anuradhapura") && dest.contains("colombo")) return 205.0;
        if (orig.contains("badulla") && dest.contains("colombo")) return 215.0;

        return 135.0;
    }
}
