package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.MarketCenterResponse;
import com.harvesthub.backend.dto.MarketTrendResponse;
import com.harvesthub.backend.model.Crop;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.MarketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api", ""})
public class MarketController {

    private final CurrentUser currentUser;
    private final MarketService marketService;

    public MarketController(CurrentUser currentUser, MarketService marketService) {
        this.currentUser = currentUser;
        this.marketService = marketService;
    }

    @GetMapping({"/market/trend", "/api/market/trend"})
    public ResponseEntity<MarketTrendResponse> getMarketTrend(
            @RequestParam(value = "crop", required = false, defaultValue = "Big Onions") String crop,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {
        return ResponseEntity.ok(marketService.getMarketTrend(crop, days));
    }

    @GetMapping({"/market/centers", "/api/market/centers"})
    public ResponseEntity<List<MarketCenterResponse>> getMarketCenters(
            @RequestParam(value = "crop", required = false) String crop) {
        return ResponseEntity.ok(marketService.getMarketCenters(crop));
    }

    /** Crop catalogue for the listing form (verified + the caller's own pending crops). */
    @GetMapping({"/crops", "/api/crops"})
    public ResponseEntity<List<Crop>> getCrops() {
        return ResponseEntity.ok(marketService.getCrops(currentUser.id()));
    }

    @GetMapping({"/wanted-ads", "/api/wanted-ads"})
    public ResponseEntity<List<WantedAd>> getWantedAds(
            @RequestParam(value = "crop", required = false) String cropFilter,
            @RequestParam(value = "search", required = false) String searchQuery) {
        return ResponseEntity.ok(marketService.getWantedAds(cropFilter, searchQuery));
    }

    @PostMapping({"/wanted-ads", "/api/wanted-ads", "/wanted-ads/create", "/api/wanted-ads/create"})
    public ResponseEntity<WantedAd> createWantedAd(@RequestBody WantedAd ad) {
        currentUser.requireRole("BUYER");
        if (ad.getCropName() == null || ad.getCropName().isBlank() || ad.getRequiredQuantityKg() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        ad.setRequestId(null); // never let a client overwrite an existing ad by id
        ad.setBuyerId(currentUser.id());
        ad.setBuyerName(currentUser.get().fullName());
        ad.setStatus("OPEN");
        return ResponseEntity.ok(marketService.createWantedAd(ad));
    }

    /** Figma buyer 2.4 "Edit Ad": only the owner, only while open with no accepted offer. */
    @PutMapping({"/wanted-ads/{adId}", "/api/wanted-ads/{adId}"})
    public ResponseEntity<WantedAd> updateWantedAd(@PathVariable String adId, @RequestBody WantedAd changes) {
        currentUser.requireRole("BUYER");
        return ResponseEntity.ok(marketService.updateWantedAd(adId, currentUser.id(), changes));
    }

    @PostMapping({"/wanted-ads/{adId}/close", "/api/wanted-ads/{adId}/close"})
    public ResponseEntity<WantedAd> closeWantedAd(@PathVariable String adId) {
        currentUser.requireRole("BUYER");
        return ResponseEntity.ok(marketService.closeWantedAd(adId, currentUser.id()));
    }

    /** body {"deadline": "yyyy-MM-dd"} */
    @PostMapping({"/wanted-ads/{adId}/republish", "/api/wanted-ads/{adId}/republish"})
    public ResponseEntity<WantedAd> republishWantedAd(@PathVariable String adId, @RequestBody java.util.Map<String, String> body) {
        currentUser.requireRole("BUYER");
        return ResponseEntity.ok(marketService.republishWantedAd(adId, currentUser.id(), body.get("deadline")));
    }

    @GetMapping({"/wanted-ads/buyer/{buyerId}", "/api/wanted-ads/buyer/{buyerId}"})
    public ResponseEntity<List<WantedAd>> getBuyerWantedAds(@PathVariable String buyerId) {
        currentUser.requireSelfOrStaff(buyerId);
        return ResponseEntity.ok(marketService.getWantedAdsByBuyer(buyerId));
    }
}
