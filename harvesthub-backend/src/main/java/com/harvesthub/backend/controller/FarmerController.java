package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.*;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.service.DashboardService;
import com.harvesthub.backend.service.FarmerProfileService;
import com.harvesthub.backend.service.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class FarmerController {

    @Autowired
    private com.harvesthub.backend.security.CurrentUser currentUser;
    @Autowired
    private com.harvesthub.backend.repository.OrderRepository orderRepository;
    @Autowired
    private FarmerProfileService farmerProfileService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private ListingService listingService;

    @GetMapping("farmer/{id}/summary")
    public ResponseEntity<FarmerSummaryResponse> getFarmerSummary(@PathVariable String id) {
        currentUser.requireSelfOrStaff(id);
        return ResponseEntity.ok(dashboardService.getFarmerSummary(id));
    }

    @GetMapping("farmer/{id}/active-listings")
    public ResponseEntity<?> getActiveListings(@PathVariable String id) {
        currentUser.requireSelfOrStaff(id);
        List<Listing> listings = listingService.getActiveListings(id);
        // purchase requests still being negotiated (the listing stays on the market until a deal is accepted)
        Map<String, List<com.harvesthub.backend.model.Order>> negotiating = orderRepository.findByFarmerId(id).stream()
                .filter(o -> "NEGOTIATING".equals(o.getStatus()) && o.getListingId() != null)
                .collect(Collectors.groupingBy(com.harvesthub.backend.model.Order::getListingId));
        // Transform to match Android's ListingItem format
        List<Map<String, Object>> result = listings.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("farmerId", l.getFarmerId());
            map.put("cropName", l.getCropName());
            map.put("location", l.getLocation());
            map.put("quantity", l.getQuantity());
            map.put("price", l.getPrice());
            map.put("createdDate", l.getCreatedDate());
            map.put("updatedTime", l.getUpdatedTime());
            map.put("status", l.getStatus() != null ? l.getStatus().trim().toUpperCase() : "PENDING");
            map.put("grade", l.getGrade());
            map.put("referenceCode", l.getReferenceCode());
            map.put("reviewNotes", l.getReviewNotes());
            map.put("activeOrderId", l.getActiveOrderId());
            map.put("cropPending", Boolean.TRUE.equals(l.getCropPending()));
            map.put("mediaUrls", l.getMediaUrls());
            List<com.harvesthub.backend.model.Order> open = negotiating.getOrDefault(l.getId(), List.of());
            if (!open.isEmpty()) {
                // newest conversation first
                com.harvesthub.backend.model.Order latest = open.stream()
                        .max(java.util.Comparator.comparing(o -> o.getCreatedDate() == null ? "" : o.getCreatedDate())).get();
                map.put("negotiationOrderId", latest.getOrderId());
                map.put("negotiationBuyerName", latest.getBuyerName());
                map.put("negotiationCount", open.size());
            }
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("farmer/{id}/listing-history")
    public ResponseEntity<?> getListingHistory(
            @PathVariable String id,
            @RequestParam(value = "search", required = false) String searchQuery) {
        currentUser.requireSelfOrStaff(id);
        List<Listing> listings = listingService.getListingHistory(id, searchQuery);
        // Transform to match Android's HistoryListing format
        List<Map<String, Object>> result = listings.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("listingId", l.getId());
            map.put("cropName", l.getCropName());
            map.put("status", l.getStatus());
            map.put("date", l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate());
            map.put("reviewNotes", l.getReviewNotes());
            map.put("buyerName", l.getBuyerName() != null ? l.getBuyerName() : "Unknown Buyer");
            map.put("quantityKg", l.getQuantityKg() != null ? l.getQuantityKg() : l.getQuantity());
            map.put("totalValue", l.getTotalValue());
            map.put("finalPricePerKg", l.getFinalPricePerKg() != null ? l.getFinalPricePerKg() : l.getPrice());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("farmer/{id}/profile")
    public ResponseEntity<?> getFarmerProfile(@PathVariable String id) {
        currentUser.requireSelfOrStaff(id);
        FarmerProfileResponse profile = farmerProfileService.getFarmerProfile(id);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    @PostMapping("farmer/{id}/update-profile")
    public ResponseEntity<?> updateFarmerProfile(
            @PathVariable String id,
            @RequestPart(value = "fullName", required = false) String fullName,
            @RequestPart(value = "phoneNumber", required = false) String phoneNumber,
            @RequestPart(value = "nic", required = false) String nic,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        currentUser.requireSelfOrStaff(id);
        boolean updated = farmerProfileService.updateProfile(id, fullName, phoneNumber, nic, photo);
        if (updated) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("farmer/location")
    public ResponseEntity<?> saveFarmLocation(@RequestBody LocationRequest request) {
        farmerProfileService.saveFarmLocation(currentUser.id(), request.getLatitude(), request.getLongitude());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Location saved successfully");
        return ResponseEntity.ok(response);
    }
}
