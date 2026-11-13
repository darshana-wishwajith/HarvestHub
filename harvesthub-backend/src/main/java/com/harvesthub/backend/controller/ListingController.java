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

    @PostMapping("/create")
    public ResponseEntity<Listing> createListing(@RequestBody Listing listing) {
        currentUser.requireRole("FARMER");
        // Server-owned fields: owner, verification state and timestamps never come from the client.
        var me = currentUser.get();
        listing.setId(null);
        listing.setFarmerId(me.id());
        listing.setFarmerName(me.fullName());
        listing.setStatus("PENDING");
        listing.setGrade(null);
        listing.setReviewNotes(null);
        listing.setClaimedByOfficerId(null);
        listing.setClaimedByOfficerName(null);
        listing.setReviewedByOfficerId(null);
        listing.setReviewedByOfficerName(null);
        listing.setReviewedAt(null);
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        listing.setCreatedDate(now);
        listing.setUpdatedTime(now);
        return ResponseEntity.ok(listingService.createListing(listing));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(@PathVariable String id, @RequestBody Listing updates) {
        Listing existing = listingService.getListingById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUser.requireSelfOrStaff(existing.getFarmerId());
        if (!"DRAFT".equals(existing.getStatus()) && !"ACTIVE".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body("Can only update DRAFT or ACTIVE listings");
        }
        return ResponseEntity.ok(listingService.updateListing(id, updates));
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
        else if ("ACTIVE".equals(currentStatus) && "DEACTIVATED".equals(newStatus)) validTransition = true;
        else if ("DEACTIVATED".equals(currentStatus) && "ACTIVE".equals(newStatus)) validTransition = true;

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
