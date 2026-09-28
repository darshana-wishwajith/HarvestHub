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
    private FarmerProfileService farmerProfileService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private ListingService listingService;

    @GetMapping("farmer/{id}/summary")
    public ResponseEntity<FarmerSummaryResponse> getFarmerSummary(@PathVariable String id) {
        return ResponseEntity.ok(dashboardService.getFarmerSummary(id));
    }

    @GetMapping("farmer/{id}/active-listings")
    public ResponseEntity<?> getActiveListings(@PathVariable String id) {
        List<Listing> listings = listingService.getActiveListings(id);
        // Transform to match Android's ListingItem format
        List<Map<String, Object>> result = listings.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("cropName", l.getCropName());
            map.put("location", l.getLocation());
            map.put("quantity", l.getQuantity());
            map.put("price", l.getPrice());
            map.put("createdDate", l.getCreatedDate());
            map.put("updatedTime", l.getUpdatedTime());
            map.put("status", l.getStatus());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("farmer/{id}/listing-history")
    public ResponseEntity<?> getListingHistory(
            @PathVariable String id,
            @RequestParam(value = "search", required = false) String searchQuery) {
        List<Listing> listings = listingService.getListingHistory(id, searchQuery);
        // Transform to match Android's HistoryListing format
        List<Map<String, Object>> result = listings.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("listingId", l.getId());
            map.put("cropName", l.getCropName());
            map.put("status", l.getStatus());
            map.put("date", l.getCreatedDate());
            map.put("buyerName", l.getBuyerName() != null ? l.getBuyerName() : "Unknown Buyer");
            map.put("quantityKg", l.getQuantityKg() != null ? l.getQuantityKg() : l.getQuantity());
            map.put("totalValue", l.getTotalValue() != null ? l.getTotalValue() : "Rs. 0");
            map.put("finalPricePerKg", l.getFinalPricePerKg() != null ? l.getFinalPricePerKg() : l.getPrice());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("farmer/{id}/profile")
    public ResponseEntity<?> getFarmerProfile(@PathVariable String id) {
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
        farmerProfileService.saveFarmLocation(request.getFarmerId(), request.getLatitude(), request.getLongitude());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Location saved successfully");
        return ResponseEntity.ok(response);
    }
}
