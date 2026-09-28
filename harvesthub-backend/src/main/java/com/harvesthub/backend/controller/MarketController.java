package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.MarketTrendResponse;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MarketController {
    @Autowired
    private MarketService marketService;

    @GetMapping("market/trend")
    public ResponseEntity<MarketTrendResponse> getMarketTrend(
            @RequestParam(value = "crop", required = false, defaultValue = "rice") String crop,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {
        return ResponseEntity.ok(marketService.getMarketTrend(crop, days));
    }

    @GetMapping("wanted-ads")
    public ResponseEntity<List<WantedAd>> getWantedAds(
            @RequestParam(value = "crop", required = false) String cropFilter,
            @RequestParam(value = "search", required = false) String searchQuery) {
        return ResponseEntity.ok(marketService.getWantedAds(cropFilter, searchQuery));
    }
}
