package com.harvesthub.backend.controller;

import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/buyer")
public class BuyerController {

	private final ListingService listingService;

	public BuyerController(ListingService listingService) {
		this.listingService = listingService;
	}

	@GetMapping("/listings")
	public ResponseEntity<List<Listing>> getMarketplaceListings(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String grade) {
		return ResponseEntity.ok(listingService.getMarketplaceListings(search, grade));
	}
}
