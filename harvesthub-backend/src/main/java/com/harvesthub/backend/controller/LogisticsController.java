package com.harvesthub.backend.controller;

import com.harvesthub.backend.service.LogisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @PostMapping({"/logistics/calculate", "/api/logistics/calculate"})
    public ResponseEntity<Map<String, Object>> calculateLogistics(@RequestBody Map<String, Object> request) {
        String origin = (String) request.getOrDefault("origin", "Nuwara Eliya");
        String destination = (String) request.getOrDefault("destination", "Colombo");
        String vehicleTier = (String) request.getOrDefault("vehicleTier", "SMALL_LORRY");

        double weightKg = 1000.0;
        Object weightObj = request.get("weightKg");
        if (weightObj instanceof Number) {
            weightKg = ((Number) weightObj).doubleValue();
        }

        Map<String, Object> response = logisticsService.calculateLogistics(origin, destination, weightKg, vehicleTier);

        Object cropTotalObj = request.get("cropTotal");
        if (cropTotalObj instanceof Number) {
            double cropTotal = ((Number) cropTotalObj).doubleValue();
            double buyerFee = Math.round(cropTotal * 0.03);
            double transportCost = ((Number) response.get("transportCost")).doubleValue();
            response.put("cropTotal", cropTotal);
            response.put("buyerConvenienceFee", buyerFee);
            response.put("totalEscrowRequired", cropTotal + transportCost + buyerFee);
        }

        return ResponseEntity.ok(response);
    }
}
