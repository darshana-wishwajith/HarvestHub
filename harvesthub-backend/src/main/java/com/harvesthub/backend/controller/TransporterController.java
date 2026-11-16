package com.harvesthub.backend.controller;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.dto.TransporterDashboardResponse;
import com.harvesthub.backend.dto.UserDto;
import com.harvesthub.backend.model.FarmLocation;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.TransporterDashboardService;
import com.harvesthub.backend.service.TransporterProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Transporter endpoints (Figma transporter view, decisions 15 Nov). */
@RestController
public class TransporterController {

    private final CurrentUser currentUser;
    private final TransporterDashboardService dashboard;
    private final UserRepository users;
    private final MarketplaceRules rules;

    public TransporterController(CurrentUser currentUser, TransporterDashboardService dashboard, UserRepository users,
                                 MarketplaceRules rules) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
        this.users = users;
        this.rules = rules;
    }

    /** Vehicle classes for the sign-up cards (public: used before the account exists). */
    @GetMapping({"/api/vehicle-types", "/vehicle-types"})
    public List<Map<String, Object>> vehicleTypes() {
        return rules.getVehicles().stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", v.getType());
            m.put("label", v.getLabel());
            m.put("capacityKg", v.getCapacityKg());
            m.put("ratePerKm", v.getRatePerKm());
            return m;
        }).toList();
    }

    @GetMapping({"/dashboard/transporter/{id}", "/api/dashboard/transporter/{id}"})
    public ResponseEntity<TransporterDashboardResponse> transporterDashboard(@PathVariable String id) {
        currentUser.requireSelfOrStaff(id);
        return ResponseEntity.ok(dashboard.dashboard(id));
    }

    /** Online / offline switch (Figma Home "Available for Hauls"). body {"online": true, "latitude"?, "longitude"?} */
    @PutMapping("/api/me/availability")
    public ResponseEntity<TransporterDashboardResponse> setAvailability(@RequestBody Map<String, Object> body) {
        currentUser.requireRole("TRANSPORTER");
        if (!(body.get("online") instanceof Boolean online)) throw new IllegalStateException("online must be true or false");
        User u = me();
        u.setOnline(online);
        if (body.get("latitude") instanceof Number la && body.get("longitude") instanceof Number lo) {
            checkSriLanka(la.doubleValue(), lo.doubleValue());
            u.setLastLatitude(la.doubleValue());
            u.setLastLongitude(lo.doubleValue());
        }
        users.save(u);
        return ResponseEntity.ok(dashboard.dashboard(u.getId()));
    }

    /**
     * Vehicle profile / base / licence (Figma "Vehicle Profile", "Set Vehicle Address", Settings). Partial update:
     * {"vehicleType", "vehiclePlate", "vehicleCapacityKg", "vehicleBodyType", "latitude", "longitude", "address", "licenceUrl"}
     */
    @PutMapping("/api/me/vehicle")
    public ResponseEntity<UserDto> updateVehicle(@RequestBody Map<String, Object> body) {
        currentUser.requireRole("TRANSPORTER");
        User u = me();
        if (body.get("vehicleType") instanceof String type) {
            if (!TransporterProfile.VEHICLE_TYPES.contains(type)) throw new IllegalStateException("Choose a vehicle type");
            if (!type.equals(u.getVehicleType()) && dashboard.hasActiveJob(u.getId())) {
                throw new IllegalStateException("Finish your current job before changing the vehicle");
            }
            u.setVehicleType(type);
        }
        if (body.get("vehiclePlate") instanceof String plate) {
            TransporterProfile.checkPlate(plate);
            u.setVehiclePlate(TransporterProfile.normalisePlate(plate));
        }
        if (body.containsKey("vehicleCapacityKg")) {
            Double cap = body.get("vehicleCapacityKg") instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : null;
            double max = rules.getVehicles().stream().filter(v -> v.getType().equals(u.getVehicleType()))
                    .mapToDouble(MarketplaceRules.Vehicle::getCapacityKg).findFirst().orElse(Double.MAX_VALUE);
            if (cap != null && cap > max) throw new IllegalStateException("Max payload is above this vehicle class (" + (int) max + " kg)");
            u.setVehicleCapacityKg(cap);
        }
        if (body.get("vehicleBodyType") instanceof String bodyType) {
            TransporterProfile.checkBody(bodyType);
            u.setVehicleBodyType(bodyType.isBlank() ? null : bodyType.trim().toUpperCase());
        }
        if (body.get("latitude") instanceof Number la && body.get("longitude") instanceof Number lo) {
            checkSriLanka(la.doubleValue(), lo.doubleValue());
            FarmLocation loc = u.getFarmLocation() == null ? new FarmLocation() : u.getFarmLocation();
            loc.setLatitude(la.doubleValue());
            loc.setLongitude(lo.doubleValue());
            loc.setAddress(body.get("address") instanceof String a && !a.isBlank() ? a.trim()
                    : String.format("%.5f, %.5f", la.doubleValue(), lo.doubleValue()));
            u.setFarmLocation(loc);
            if (u.getLastLatitude() == null) {
                u.setLastLatitude(la.doubleValue());
                u.setLastLongitude(lo.doubleValue());
            }
        }
        if (body.get("licenceUrl") instanceof String url) {
            // only files stored by our own upload endpoint
            if (!url.startsWith("/uploads/") || url.contains("..")) throw new IllegalStateException("Upload the licence photo first");
            u.setLicenceUrl(url);
            u.setLicenceUploadedAt(java.time.LocalDateTime.now().toString());
        }
        users.save(u);
        return ResponseEntity.ok(UserDto.from(u));
    }

    private User me() {
        return users.findById(currentUser.id()).orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private static void checkSriLanka(double la, double lo) {
        if (la < 5.8 || la > 10.0 || lo < 79.4 || lo > 82.1) throw new IllegalStateException("The location must be in Sri Lanka");
    }
}
