package com.harvesthub.backend.controller;

import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.service.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class BuyerListingController {

    @Autowired
    private ListingService listingService;

    @GetMapping({"/buyer/listings", "/api/buyer/listings"})
    public ResponseEntity<List<Listing>> getBuyerListings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String grade) {
        return ResponseEntity.ok(listingService.getMarketplaceListings(search, grade));
    }
}
