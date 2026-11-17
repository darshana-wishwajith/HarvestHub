package com.harvesthub.backend.controller;

import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.service.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/listings")
public class ListingController {

    @Autowired
    private com.harvesthub.backend.security.CurrentUser currentUser;

    @Autowired
    private ListingService listingService;

    /**
     * Workflow 1.2–1.4: location-locked, geotagged submission. Body must carry gpsLatitude/gpsLongitude
     * (device position at capture) and mediaUrls [wide photo, close-up photo, 10 s video] from /media/upload.
     */
    @PostMapping("/create")
    public ResponseEntity<Listing> createListing(@RequestBody Listing listing) {
        currentUser.requireRole("FARMER");
        return ResponseEntity.ok(listingService.submitListing(listing, currentUser.id(), null));
    }

    /** Officer asked for a retake (RETAKE_REQUESTED): farmer re-captures media at the farm → PENDING again. */
    @PutMapping("/{id}/retake")
    public ResponseEntity<Listing> retake(@PathVariable String id, @RequestBody Listing capture) {
        currentUser.requireRole("FARMER");
        return ResponseEntity.ok(listingService.submitListing(capture, currentUser.id(), id));
    }

    /** Location-Lock screen (Figma 12): is the device at the registered farm? */
    @GetMapping("/location-check")
    public ResponseEntity<Map<String, Object>> locationCheck(@RequestParam double lat, @RequestParam double lng) {
        currentUser.requireRole("FARMER");
        return ResponseEntity.ok(listingService.locationCheck(currentUser.id(), lat, lng));
    }

    /** Farmer card "Edit": estimated yield and price per kg only (crop and media were verified). */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(@PathVariable String id, @RequestBody Listing updates) {
        currentUser.requireRole("FARMER");
        return ResponseEntity.ok(listingService.editYieldAndPrice(id, currentUser.id(), updates.getQuantity(), updates.getPrice()));
    }

    /** Farmer card "Deactivate": hidden from the marketplace and the officer queue. */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Listing> deactivate(@PathVariable String id) {
        currentUser.requireRole("FARMER");
        return ResponseEntity.ok(listingService.deactivate(id, currentUser.id()));
    }

    /** Farmer card "Re-activate": back to Listed (same grade) or to the officer queue. */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Listing> reactivate(@PathVariable String id) {
        currentUser.requireRole("FARMER");
        return ResponseEntity.ok(listingService.reactivate(id, currentUser.id()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        Listing existing = listingService.getListingById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUser.requireSelfOrStaff(existing.getFarmerId());
        String currentStatus = existing.getStatus();
        boolean validTransition = false;
        
        if ("DRAFT".equals(currentStatus) && "PENDING".equals(newStatus)) validTransition = true; // submit for verification

        if (!validTransition) {
            return ResponseEntity.badRequest().body("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
        return ResponseEntity.ok(listingService.changeStatus(id, newStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable String id) {
        Listing existing = listingService.getListingById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUser.requireSelfOrStaff(existing.getFarmerId());
        if (!"DRAFT".equals(existing.getStatus()) && !"DEACTIVATED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body("Can only delete DRAFT or DEACTIVATED listings");
        }
        listingService.deleteListing(id);
        return ResponseEntity.ok().build();
    }
}
