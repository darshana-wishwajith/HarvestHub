package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.BuyerDashboardResponse;
import com.harvesthub.backend.model.FarmLocation;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.BuyerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Buyer-only endpoints. */
@RestController
public class BuyerController {

    private final CurrentUser currentUser;
    private final BuyerDashboardService dashboard;
    private final UserRepository users;

    public BuyerController(CurrentUser currentUser, BuyerDashboardService dashboard, UserRepository users) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
        this.users = users;
    }

    @GetMapping({"/dashboard/buyer/{id}", "/api/dashboard/buyer/{id}"})
    public ResponseEntity<BuyerDashboardResponse> buyerDashboard(@PathVariable String id) {
        currentUser.requireSelfOrStaff(id);
        return ResponseEntity.ok(dashboard.dashboard(id));
    }

    /**
     * Buyer delivery location. body {"latitude", "longitude", "address"}.
     * Farmers keep using their registered farm location (listing location lock), so this is buyer-only.
     */
    @PutMapping("/api/me/delivery-location")
    public ResponseEntity<Map<String, Object>> setDeliveryLocation(@RequestBody Map<String, Object> body) {
        currentUser.requireRole("BUYER");
        if (!(body.get("latitude") instanceof Number lat) || !(body.get("longitude") instanceof Number lng)) {
            throw new IllegalStateException("Pick a point on the map");
        }
        double la = lat.doubleValue(), lo = lng.doubleValue();
        // Sri Lanka bounding box (service area)
        if (la < 5.8 || la > 10.0 || lo < 79.4 || lo > 82.1) throw new IllegalStateException("The delivery point must be in Sri Lanka");
        String address = body.get("address") instanceof String a && !a.isBlank() ? a.trim()
                : String.format("%.5f, %.5f", la, lo);
        User u = users.findById(currentUser.id()).orElseThrow(() -> new IllegalStateException("User not found"));
        FarmLocation loc = u.getFarmLocation() == null ? new FarmLocation() : u.getFarmLocation();
        loc.setLatitude(la);
        loc.setLongitude(lo);
        loc.setAddress(address);
        u.setFarmLocation(loc);
        users.save(u);
        return ResponseEntity.ok(Map.of("latitude", la, "longitude", lo, "address", address));
    }
}
