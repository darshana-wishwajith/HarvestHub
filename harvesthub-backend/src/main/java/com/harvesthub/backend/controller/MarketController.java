package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.MarketTrendResponse;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api", ""})
public class MarketController {
    @Autowired
    private MarketService marketService;

    @GetMapping({"/market/trend", "/api/market/trend"})
    public ResponseEntity<MarketTrendResponse> getMarketTrend(
            @RequestParam(value = "crop", required = false, defaultValue = "rice") String crop,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {
        return ResponseEntity.ok(marketService.getMarketTrend(crop, days));
    }

    @GetMapping({"/wanted-ads", "/api/wanted-ads"})
    public ResponseEntity<List<WantedAd>> getWantedAds(
            @RequestParam(value = "crop", required = false) String cropFilter,
            @RequestParam(value = "search", required = false) String searchQuery) {
        return ResponseEntity.ok(marketService.getWantedAds(cropFilter, searchQuery));
    }

    @GetMapping({"/market/centers", "/api/market/centers"})
    public ResponseEntity<List<java.util.Map<String, Object>>> getMarketCenters() {
        return ResponseEntity.ok(marketService.getMarketCenters());
    }

    @PostMapping({"/wanted-ads", "/api/wanted-ads", "/wanted-ads/create", "/api/wanted-ads/create"})
    public ResponseEntity<WantedAd> createWantedAd(@RequestBody WantedAd ad) {
        return ResponseEntity.ok(marketService.createWantedAd(ad));
    }

    @GetMapping({"/wanted-ads/buyer/{buyerId}", "/api/wanted-ads/buyer/{buyerId}"})
    public ResponseEntity<List<WantedAd>> getBuyerWantedAds(@PathVariable String buyerId) {
        return ResponseEntity.ok(marketService.getWantedAdsByBuyer(buyerId));
    }
}